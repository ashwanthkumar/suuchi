package in.ashwanthkumar.suuchi.models

/**
 * A dataset is a global container to store multiple versions of a processed data. Every dataset is referenced
 * by a name - "Users". Each dataset can contain multiple versions. Upon deployment it should be easy to swap versions
 * of the dataset on the DB.
 *
 */
case class Dataset(name: String,
                   createdAt: Long,
                   versions: List[Batch] = Nil,
                   blacklistedVersions: List[String] = Nil)


/**
 * A Batch contains a version of processed output set. Each batch is referenced with an Id. Each batch can contain
 * multiple Microbatches. We can also attach a purge policy to a batch to determine when it is supposed to clear itself.
 *
 */
case class Batch(id: String,
                 loadedAt: Long,
                 batches: List[MicroBatch],
                 purgePolicy: PurgePolicy = NoPurgePolicy,
                 blackListed: List[String] = Nil)

/**
 * A microbatch is a smaller increment to a batch. In comparison to the world of Lambda Architecture, they would be like
 *
 * | Lambda Architecture                         | Suuchi     |
 * |---------------------------------------------|------------|
 * | Users / Tweets or other datamodel           | Dataset    |
 * | A version of processed output in your stage | Batch      |
 * | Hourly processed MasterData                 | MicroBatch |
 *  ----------------------------------------------------------
 */
case class MicroBatch(timestamp: Long,
                      id: Option[String] = None,
                      purgePolicy: PurgePolicy = NoPurgePolicy)
