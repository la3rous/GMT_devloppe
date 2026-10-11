package com.gmt.rental.repository;

import com.gmt.rental.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SettingRepository extends JpaRepository<Setting, Long> {
}
