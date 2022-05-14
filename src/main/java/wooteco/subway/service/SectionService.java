package wooteco.subway.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wooteco.subway.dao.LineDao;
import wooteco.subway.dao.SectionDao;
import wooteco.subway.dao.StationDao;
import wooteco.subway.domain.Section;
import wooteco.subway.domain.Sections;
import wooteco.subway.domain.Station;
import wooteco.subway.exception.DataNotFoundException;
import java.util.List;
import java.util.Optional;

@Service
public class SectionService {

    private final SectionDao sectionDao;
    private final StationDao stationDao;
    private final LineDao lineDao;

    public SectionService(final SectionDao sectionDao, final StationDao stationDao, final LineDao lineDao) {
        this.sectionDao = sectionDao;
        this.stationDao = stationDao;
        this.lineDao = lineDao;
    }

    @Transactional
    public Section addSection(final long lineId, final Section section) {
        final Station upStation = stationDao.findById(section.getUpStation().getId())
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 지하철역 ID입니다."));
        final Station downStation = stationDao.findById(section.getDownStation().getId())
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 지하철역 ID입니다."));
        lineDao.findById(lineId)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 노선 ID입니다."));

        Section sectionToSave = new Section(upStation, downStation, section.getDistance(), lineId);

        final List<Section> lineSections = sectionDao.findAllByLineId(lineId);
        final Sections sections = new Sections(lineSections);
        sections.add(sectionToSave);

        lineSections.add(sectionToSave);
        List<Section> sectionsToUpdate = sections.deleteAll(lineSections);
        for (Section sectionToUpdate : sectionsToUpdate) {
            sectionDao.update(sectionToUpdate.getId(), sectionToUpdate);
        }

        return sectionDao.save(sectionToSave);
    }

    @Transactional
    public void delete(final Long lineId, final Long stationId) {
        final Sections sections = new Sections(sectionDao.findAllByLineId(lineId));
        final List<Section> sectionsToDelete = sections.pop(stationId);
        final Optional<Section> mergedSection = sections.findMergedSection(sectionsToDelete);
        sectionDao.deleteAll(sectionsToDelete);
        mergedSection.ifPresent(sectionDao::save);
    }

    @Transactional(readOnly = true)
    public List<Section> getSectionsByLine(final long lineId) {
        return sectionDao.findAllByLineId(lineId);
    }

    @Transactional(readOnly = true)
    public List<Station> getStationsByLine(final long lineId) {
        final List<Section> lineSections = sectionDao.findAllByLineId(lineId);
        final Sections sections = new Sections(lineSections);
        return sections.extractStations();
    }
}
