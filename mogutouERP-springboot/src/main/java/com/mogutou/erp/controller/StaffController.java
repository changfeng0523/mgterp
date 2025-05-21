package com.mogutou.erp.controller;

import com.mogutou.erp.common.Result;
import com.mogutou.erp.entity.Company;
import com.mogutou.erp.entity.Staff;
import com.mogutou.erp.service.CompanyService;
import com.mogutou.erp.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private StaffService staffService;
    
    @Autowired
    private CompanyService companyService;

    @GetMapping
    public Result<?> getStaffList(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Staff> staffPage = staffService.getAllStaff(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", staffPage.getContent());
        response.put("totalElements", staffPage.getTotalElements());
        response.put("totalPages", staffPage.getTotalPages());
        response.put("number", staffPage.getNumber());
        response.put("size", staffPage.getSize());
        
        return Result.success(response);
    }
    
    @GetMapping("/company/{companyId}")
    public Result<?> getStaffByCompany(
            @PathVariable Long companyId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        
        // 检查公司是否存在
        Optional<Company> company = companyService.getCompanyById(companyId);
        if (!company.isPresent()) {
            return Result.error("公司不存在");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Staff> staffPage = staffService.getStaffByCompany(companyId, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", staffPage.getContent());
        response.put("totalElements", staffPage.getTotalElements());
        response.put("totalPages", staffPage.getTotalPages());
        response.put("number", staffPage.getNumber());
        response.put("size", staffPage.getSize());
        
        return Result.success(response);
    }
    
    @GetMapping("/{id}")
    public Result<?> getStaffDetail(@PathVariable Long id) {
        Optional<Staff> staff = staffService.getStaffById(id);
        
        if (staff.isPresent()) {
            return Result.success(staff.get());
        } else {
            return Result.error("员工不存在");
        }
    }

    @PostMapping
    public Result<?> createStaff(@RequestBody Staff staff) {
        try {
            Staff savedStaff = staffService.createStaff(staff);
            return Result.success("员工创建成功", savedStaff);
        } catch (Exception e) {
            return Result.error("创建员工失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<?> updateStaff(
            @PathVariable Long id,
            @RequestBody Staff staff) {
        try {
            Staff updatedStaff = staffService.updateStaff(id, staff);
            return Result.success("员工更新成功", updatedStaff);
        } catch (Exception e) {
            return Result.error("更新员工失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteStaff(@PathVariable Long id) {
        try {
            staffService.deleteStaff(id);
            return Result.success("员工删除成功");
        } catch (Exception e) {
            return Result.error("删除员工失败: " + e.getMessage());
        }
    }
}