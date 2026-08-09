package com.medical.dto;

import lombok.Data;
import java.util.Map;

/**
 * 护士复核请求DTO
 */
@Data
public class NurseCorrectionRequest {
    
    /**
     * 边缘数据ID
     */
    private Long edgeDataId;
    
    /**
     * 护士ID
     */
    private Long nurseId;
    
    /**
     * 护士姓名
     */
    private String nurseName;
    
    /**
     * 操作类型: "SEND_TO_EDGE"(发回边缘重新分诊) 或 "CONFIRM_TO_CLOUD"(确认提交云端)
     */
    private String action;
    
    /**
     * 修正/确认后的传感器数据（生命体征）
     */
    private Map<String, Object> correctedSensorData;
    
    /**
     * 修正/确认后的主诉（语音主诉文本）
     */
    private String correctedChiefComplaint;
    
    /**
     * 护士备注
     */
    private String nurseNotes;
    
    // 手动添加getter方法（解决IDE Lombok识别问题）
    public Long getEdgeDataId() { return edgeDataId; }
    public Long getNurseId() { return nurseId; }
    public String getNurseName() { return nurseName; }
    public String getAction() { return action; }
    public Map<String, Object> getCorrectedSensorData() { return correctedSensorData; }
    public String getCorrectedChiefComplaint() { return correctedChiefComplaint; }
    public String getNurseNotes() { return nurseNotes; }
    
    public void setEdgeDataId(Long edgeDataId) { this.edgeDataId = edgeDataId; }
    public void setNurseId(Long nurseId) { this.nurseId = nurseId; }
    public void setNurseName(String nurseName) { this.nurseName = nurseName; }
    public void setAction(String action) { this.action = action; }
    public void setCorrectedSensorData(Map<String, Object> data) { this.correctedSensorData = data; }
    public void setCorrectedChiefComplaint(String complaint) { this.correctedChiefComplaint = complaint; }
    public void setNurseNotes(String notes) { this.nurseNotes = notes; }
}
