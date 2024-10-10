package nl.surepay.validator.repository;

import nl.surepay.validator.entity.Batch;
import org.springframework.data.repository.CrudRepository;

public interface BatchRepository extends CrudRepository<Batch, Long> {
}
