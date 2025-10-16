package com.example.pgrown30.mapper;

import com.example.pgrown30.domain.CitizenServiceEntity;
import com.example.pgrown30.domain.Status;
import com.example.pgrown30.web.models.CitizenService;

public class CitizenServiceMapper {

    public static CitizenServiceEntity toEntity(CitizenService dto) {
        if (dto == null) return null;

        CitizenServiceEntity entity = new CitizenServiceEntity();
        entity.setServiceRequestId(dto.getServiceRequestId());
        entity.setTenantId(dto.getTenantId());
        entity.setServiceCode(dto.getServiceCode());
        entity.setDescription(dto.getDescription());
        entity.setAccountId(dto.getAccountId());
        entity.setSource(dto.getSource());
        entity.setApplicationStatus(dto.getApplicationStatus() != null 
                ? Status.valueOf(dto.getApplicationStatus()) 
                : Status.INITIATED);
        entity.setFileStoreId(dto.getFileStoreId());
        entity.setFileValid(dto.isFileValid());
        entity.setBoundaryCode(dto.getBoundaryCode());
        entity.setAction(dto.getAction());
        entity.setWorkflowInstanceId(dto.getWorkflowInstanceId());
        entity.setCreatedTime(dto.getCreatedTime());
        entity.setLastModifiedTime(dto.getLastModifiedTime());
        entity.setEmail(dto.getEmail());       // Add email
        entity.setMobile(dto.getMobile());     // Add mobile
        return entity;
    }

    public static CitizenService toDto(CitizenServiceEntity entity) {
        if (entity == null) return null;

        CitizenService dto = new CitizenService();
        dto.setServiceRequestId(entity.getServiceRequestId());
        dto.setTenantId(entity.getTenantId());
        dto.setServiceCode(entity.getServiceCode());
        dto.setDescription(entity.getDescription());
        dto.setAccountId(entity.getAccountId());
        dto.setSource(entity.getSource());
        dto.setApplicationStatus(entity.getApplicationStatus() != null 
                ? entity.getApplicationStatus().name() 
                : null);
        dto.setFileStoreId(entity.getFileStoreId());
        dto.setFileValid(entity.getFileValid());
        dto.setBoundaryCode(entity.getBoundaryCode());
        dto.setAction(entity.getAction());
        dto.setWorkflowInstanceId(entity.getWorkflowInstanceId());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setLastModifiedTime(entity.getLastModifiedTime());
        dto.setBoundaryValid(entity.isBoundaryValid());
        dto.setEmail(entity.getEmail());       // Add email
        dto.setMobile(entity.getMobile());     // Add mobile
        return dto;
    }
}
