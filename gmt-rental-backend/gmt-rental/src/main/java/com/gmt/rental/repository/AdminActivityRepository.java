package com.gmt.rental.repository;

import com.gmt.rental.entity.AdminActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminActivityRepository extends JpaRepository<AdminActivity, Long>, JpaSpecificationExecutor<AdminActivity> {}
