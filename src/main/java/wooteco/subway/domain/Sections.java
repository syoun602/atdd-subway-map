package wooteco.subway.domain;

import wooteco.subway.domain.exception.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Sections {

    private static final int MINIMUM_SIZE = 1;
    private static final int NEEDS_MERGE_SIZE = 2;

    private final List<Section> value;

    public Sections(final List<Section> value) {
        this.value = new ArrayList<>(value);
    }

    public void add(final Section section) {
        validateCanAdd(section);

        findUpSection(section).ifPresent(it -> update(section, it));
        findDownSection(section).ifPresent(it -> update(section, it));
        value.add(section);
    }

    public List<Section> deleteAll(final List<Section> sections) {
        List<Section> origin = new ArrayList<>(value);
        origin.removeAll(sections);
        return origin;
    }

    public List<Section> pop(final long stationId) {
        final List<Section> sections = findSectionByStationId(stationId);
        validateMinimumSize();
        validateSectionNotFound(sections);

        value.removeAll(sections);

        if (sections.size() != NEEDS_MERGE_SIZE) {
            return sections;
        }

        mergeSections(sections);
        return sections;
    }

    public Optional<Section> findMergedSection(final List<Section> sections) {
        if (sections.size() == NEEDS_MERGE_SIZE) {
            return mergeSection(sections);
        }
        return Optional.empty();
    }

    public List<Station> extractStations() {
        return Stream.concat(getStations(Section::getUpStation), getStations(Section::getDownStation)).distinct().collect(Collectors.toList());
    }

    private Optional<Section> mergeSection(final List<Section> sections) {
        final Section section1 = sections.get(0);
        final Section section2 = sections.get(1);

        if (section1.getDownStation().equals(section2.getUpStation())) {
            return findSection(section1.merge(section2));
        }
        if (section1.getUpStation().equals(section2.getDownStation())) {
            return findSection(section2.merge(section1));
        }
        throw new CannotMergeException();
    }

    private void update(final Section source, final Section target) {
        value.remove(target);
        value.add(target.createSectionInBetween(source));
    }

    private List<Section> findSectionByStationId(final long stationId) {
        return value.stream().filter(section -> section.getUpStation().getId() == stationId || section.getDownStation().getId() == stationId).collect(Collectors.toList());
    }

    private void validateSectionNotFound(final List<Section> sections) {
        if (sections.size() == 0) {
            throw new SectionNotFoundException();
        }
    }

    private void validateMinimumSize() {
        if (value.size() <= MINIMUM_SIZE) {
            throw new IllegalSectionDeleteBySizeException();
        }
    }

    private void mergeSections(final List<Section> sections) {
        final Section section1 = sections.get(0);
        final Section section2 = sections.get(1);

        if (section1.getDownStation().equals(section2.getUpStation())) {
            value.add(section1.merge(section2));
        }
        if (section1.getUpStation().equals(section2.getDownStation())) {
            value.add(section2.merge(section1));
        }
    }

    private Optional<Section> findSection(final Section section) {
        return value.stream().filter(it -> it.equals(section)).findAny();
    }

    private void validateCanAdd(final Section other) {
        validateSectionInsertion(other, extractStations());
        validateUpSection(other);
        validateDownSection(other);
    }

    private void validateSectionInsertion(final Section other, final List<Station> stations) {
        final boolean hasUpStation = stations.contains(other.getUpStation());
        final boolean hasDownStation = stations.contains(other.getDownStation());

        if (hasUpStation && hasDownStation) {
            throw new SectionAlreadyExistsException();
        }
        if (!hasUpStation && !hasDownStation) {
            throw new NoStationExistsException();
        }
    }

    private void validateUpSection(final Section other) {
        final Optional<Section> upSection = findUpSection(other);

        upSection.ifPresent(it -> validateDistance(it, other));
    }

    private void validateDownSection(final Section other) {
        final Optional<Section> downSection = findDownSection(other);

        downSection.ifPresent(it -> validateDistance(it, other));
    }

    private Optional<Section> findDownSection(final Section other) {
        return value.stream().filter(it -> it.getDownStation().equals(other.getDownStation())).findAny();
    }

    private Optional<Section> findUpSection(final Section other) {
        return value.stream().filter(it -> it.getUpStation().equals(other.getUpStation())).findAny();
    }

    private void validateDistance(final Section section, final Section other) {
        if (other.isGreaterOrEqualTo(section)) {
            throw new DistanceTooLongException();
        }
    }

    private Stream<Station> getStations(Function<Section, Station> function) {
        return value.stream().map(function);
    }

    public List<Section> getSections() {
        return value;
    }

    @Override
    public String toString() {
        return "Sections{" + "value=" + value + '}';
    }
}
