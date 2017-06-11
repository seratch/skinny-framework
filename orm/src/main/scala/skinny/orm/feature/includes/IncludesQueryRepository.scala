package skinny.orm.feature.includes

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

import skinny.orm.feature.associations._

import scala.collection.mutable.{ Map => MutableMap }
import scala.collection.concurrent.{ TrieMap => MutableTrieMap }

/**
  * Entity repository for includes queries.
  *
  * @tparam Entity base entity type
  */
trait IncludesQueryRepository[Entity] extends Closeable {

  private[this] val belongsTo: MutableMap[BelongsToExtractor[Entity], Seq[_]] = new MutableTrieMap()
  private[this] val hasOne: MutableMap[HasOneExtractor[Entity], Seq[_]]       = new MutableTrieMap()
  private[this] val hasMany: MutableMap[HasManyExtractor[Entity], Seq[_]]     = new MutableTrieMap()

  private[this] val allCacheEntries: Seq[MutableMap[_ <: Extractor[Entity], Seq[_]]] = Seq(
    belongsTo,
    hasOne,
    hasMany
  )

  private[this] val alreadyClosed: AtomicBoolean = new AtomicBoolean(false)
  private[this] val errorMessage = "The cache repository for #includes is unexpectedly abandoned. " +
  "Please report the stacktrace at https://github.com/skinny-framework/skinny-framework"

  private[this] def ensureTheStateAndThrowExceptionIfInvalid(): Unit = {
    if (alreadyClosed.get()) {
      throw new IllegalStateException(errorMessage)
    }
  }

  override def close(): Unit = {
    // To avoid memory leak issue when calling many finder/querying APIs which have #includes to solve huge data set.
    allCacheEntries.foreach { cache =>
      cache.clear()
    }
    alreadyClosed.set(true)
  }

  /**
    * Returns entities for belongsTo relation.
    *
    * @param extractor extractor
    * @return entities
    */
  def entitiesFor(extractor: BelongsToExtractor[Entity]): Seq[_] = {
    ensureTheStateAndThrowExceptionIfInvalid()
    belongsTo.getOrElse(extractor, Nil)
  }

  /**
    * Returns entities for hasOne relation.
    *
    * @param extractor extractor
    * @return entities
    */
  def entitiesFor(extractor: HasOneExtractor[Entity]): Seq[_] = {
    ensureTheStateAndThrowExceptionIfInvalid()
    hasOne.getOrElse(extractor, Nil)
  }

  /**
    * Returns entities for hasMany relation.
    *
    * @param extractor extractor
    * @return entities
    */
  def entitiesFor(extractor: HasManyExtractor[Entity]): Seq[_] = {
    ensureTheStateAndThrowExceptionIfInvalid()
    hasMany.getOrElse(extractor, Nil)
  }

  /**
    * Put an entity to repository.
    *
    * @param extractor extractor
    * @param entity entity
    */
  def putAndReturn[A](extractor: BelongsToExtractor[Entity], entity: A): A = {
    belongsTo.update(extractor, belongsTo.getOrElse(extractor, Nil).+:(entity))
    entity
  }

  /**
    * Put an entity to repository.
    *
    * @param extractor extractor
    * @param entity entity
    */
  def putAndReturn[A](extractor: HasOneExtractor[Entity], entity: A): A = {
    ensureTheStateAndThrowExceptionIfInvalid()
    hasOne.update(extractor, hasOne.getOrElse(extractor, Nil).+:(entity))
    entity
  }

  /**
    * Put an entity to repository.
    *
    * @param extractor extractor
    * @param entity entity
    */
  def putAndReturn[A](extractor: HasManyExtractor[Entity], entity: A): A = {
    ensureTheStateAndThrowExceptionIfInvalid()
    hasMany.update(extractor, hasMany.getOrElse(extractor, Nil).+:(entity))
    entity
  }
}

/**
  * IncludesQueryRepository factory.
  */
object IncludesQueryRepository {

  def apply[Entity](): IncludesQueryRepository[Entity] = {
    val newRepository = new DefaultIncludesQueryRepository[Entity]
    newRepository
  }
}

/**
  * Default implementation.
  *
  * @tparam Entity base entity type
  */
class DefaultIncludesQueryRepository[Entity] extends IncludesQueryRepository[Entity]
