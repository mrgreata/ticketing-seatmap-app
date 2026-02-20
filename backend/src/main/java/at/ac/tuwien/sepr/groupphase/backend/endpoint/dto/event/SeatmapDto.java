package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatmapSeatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.stage.StageSegmentDto;

import java.util.List;

public class SeatmapDto {

    private Long eventId;
    private List<SeatmapSeatDto> seats;
    private String stagePosition; // TOP, BOTTOM, LEFT, RIGHT
    private String stageLabel;
    private Integer stageRowStart;
    private Integer stageRowEnd;
    private Integer stageColStart;
    private Integer stageColEnd;
    private List<StageSegmentDto> stageSegments;

    public List<StageSegmentDto> getStageSegments() {
        return stageSegments;
    }

    public void setStageSegments(List<StageSegmentDto> stageSegments) {
        this.stageSegments = stageSegments;
    }


    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public List<SeatmapSeatDto> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatmapSeatDto> seats) {
        this.seats = seats;
    }

    public String getStagePosition() {
        return stagePosition;
    }

    public void setStagePosition(String stagePosition) {
        this.stagePosition = stagePosition;
    }

    public String getStageLabel() {
        return stageLabel;
    }

    public void setStageLabel(String stageLabel) {
        this.stageLabel = stageLabel;
    }

    public Integer getStageRowStart() {
        return stageRowStart;
    }

    public void setStageRowStart(Integer stageRowStart) {
        this.stageRowStart = stageRowStart;
    }

    public Integer getStageRowEnd() {
        return stageRowEnd;
    }

    public void setStageRowEnd(Integer stageRowEnd) {
        this.stageRowEnd = stageRowEnd;
    }

    public Integer getStageColStart() {
        return stageColStart;
    }

    public void setStageColStart(Integer stageColStart) {
        this.stageColStart = stageColStart;
    }

    public Integer getStageColEnd() {
        return stageColEnd;
    }

    public void setStageColEnd(Integer stageColEnd) {
        this.stageColEnd = stageColEnd;
    }

    private Integer stageHeightPx;
    private Integer stageWidthPx;

    public Integer getStageHeightPx() {
        return stageHeightPx;
    }

    public void setStageHeightPx(Integer stageHeightPx) {
        this.stageHeightPx = stageHeightPx;
    }

    public Integer getStageWidthPx() {
        return stageWidthPx;
    }

    public void setStageWidthPx(Integer stageWidthPx) {
        this.stageWidthPx = stageWidthPx;
    }

    private Integer runwayWidthPx;
    private Integer runwayLengthPx;
    private Integer runwayOffsetPx;

    public Integer getRunwayWidthPx() {
        return runwayWidthPx;
    }

    public void setRunwayWidthPx(Integer runwayWidthPx) {
        this.runwayWidthPx = runwayWidthPx;
    }

    public Integer getRunwayLengthPx() {
        return runwayLengthPx;
    }

    public void setRunwayLengthPx(Integer runwayLengthPx) {
        this.runwayLengthPx = runwayLengthPx;
    }

    public Integer getRunwayOffsetPx() {
        return runwayOffsetPx;
    }

    public void setRunwayOffsetPx(Integer runwayOffsetPx) {
        this.runwayOffsetPx = runwayOffsetPx;
    }

}
