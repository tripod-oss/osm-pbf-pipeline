/*
 *********************************************************************************
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <nico@beerfactory.org> wrote this file.  As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.   Nicolas JOUANIN
 *********************************************************************************
 */
package io.tripod.pipeline.osm.pbf

import akka.stream.scaladsl.{Flow, Keep, Source}
import akka.util.ByteString
import io.tripod.pipeline.osm.pbf.model.{HeaderEntity, OSMEntity}

import scala.concurrent.{ExecutionContext, Future}

object PbfStreamFlow {
  def apply()(implicit ec: ExecutionContext): Flow[ByteString, OSMEntity, Future[HeaderEntity]] = apply(1)

  def apply(parallelism: Int)(implicit ec: ExecutionContext): Flow[ByteString, OSMEntity, Future[HeaderEntity]] =
    PbfReaderStage()
      .via(UncompressStage(parallelism))
      .viaMat(FileBlockDecodeStage(parallelism))(Keep.right)
      .viaMat(EntityExtractionStage(parallelism))(Keep.left)

  /**
    * Build a PBF reader composite source from a ByteString stream
    * @param source source of PBF stream ByteString
    * @param parallelism degree of stream parallelism (defaults to 1)
    * @param ec
    * @return an OSMEntity Stream Source
    */
  def from(source: Source[ByteString, _], parallelism: Int = 1)(implicit ec: ExecutionContext) =
    source.viaMat(PbfStreamFlow(parallelism))(Keep.left).named("pbfStreamSource")
}
