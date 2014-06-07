package io.prediction.storage

import io.prediction.storage.MongoUtils.{
  emptyObj,
  mongoDbListToListOfString,
  idWithAppid
}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import com.github.nscala_time.time.Imports._

class MongoItemSets(db: MongoDB) extends ItemSets {
  private val itemSetColl = db("itemSets")

  RegisterJodaTimeConversionHelpers()

  private def dbObjToItemSet(dbObj: DBObject) = {
    val appid = dbObj.as[Int]("appid")
    ItemSet(
      id = dbObj.as[String]("_id").drop(appid.toString.length + 1),
      appid = appid,
      iids = mongoDbListToListOfString(dbObj.as[MongoDBList]("iids"))
        .map(_.drop(appid.toString.length + 1)),
      t = dbObj.getAs[DateTime]("t")
    )
  }

  private class MongoItemSetsIterator(it: MongoCursor)
      extends Iterator[ItemSet] {
    def next = dbObjToItemSet(it.next)
    def hasNext = it.hasNext
  }

  /** Insert new ItemSet */
  def insert(itemSet: ItemSet): Unit = {
    val req = MongoDBObject(
      "_id" -> idWithAppid(itemSet.appid, itemSet.id),
      "appid" -> itemSet.appid,
      "iids" -> itemSet.iids.map(i => idWithAppid(itemSet.appid, i)))
    val opt = itemSet.t.map(t => MongoDBObject("t" -> t)).getOrElse(emptyObj)
    itemSetColl.save(req ++ opt)
  }

  /** Get an item set */
  def get(appid: Int, id: String): Option[ItemSet] = {
    itemSetColl.findOne(MongoDBObject("_id" -> idWithAppid(appid, id)))
      .map(dbObjToItemSet(_))
  }

  /** Get by appid */
  def getByAppid(appid: Int): Iterator[ItemSet] = {
    new MongoItemSetsIterator(itemSetColl.find(MongoDBObject("appid" -> appid)))
  }

  /** Get by appid and t >= startTime and t < untilTime */
  def getByAppidAndTime(appid: Int, startTime: DateTime, untilTime: DateTime):
    Iterator[ItemSet] = {
      new MongoItemSetsIterator(itemSetColl.find(
        MongoDBObject("appid" -> appid,
          "t" -> MongoDBObject("$gte" -> startTime, "$lt" -> untilTime))))
    }

  /** Delete itemSet */
  def delete(itemSet: ItemSet): Unit = {
    itemSetColl.remove(MongoDBObject("_id" -> idWithAppid(itemSet.appid,
      itemSet.id)))
  }

  /** Delete by appid */
  def deleteByAppid(appid: Int): Unit = {
    itemSetColl.remove(MongoDBObject("appid" -> appid))
  }

}