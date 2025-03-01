package com.corefit.repository;

import com.corefit.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepo extends JpaRepository<City, Long> {
    public City findById(long id);

    public List<City> findAllByGovernorateId(long governorateId);
}
