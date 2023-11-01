package com.surf.diagram.diagram.repository;

import com.surf.diagram.diagram.entity.Diagram;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DiagramRepository extends MongoRepository<Diagram, Long> {
    List<Diagram> findByUserId(int userId);
}
