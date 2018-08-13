package com.github.propi.rdfrules.data.formats

import java.io.{BufferedInputStream, BufferedOutputStream}

import com.github.propi.rdfrules
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.utils.{InputStreamBuilder, OutputStreamBuilder}
import org.apache.jena.graph
import org.apache.jena.graph.{Node_Blank, Node_Literal, Node_URI}
import org.apache.jena.riot.system.{StreamRDF, StreamRDFWriter}
import org.apache.jena.riot.{Lang, RDFDataMgr, RDFFormat, RDFLanguages}
import org.apache.jena.sparql.core.Quad

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 14. 1. 2018.
  */
trait JenaLang {

  private class StreamRdfImpl[U](f: rdfrules.data.Quad => U) extends StreamRDF {
    private val prefixes = collection.mutable.ListBuffer.empty[Prefix]

    private def uriToTripleItem(x: Node_URI): TripleItem.Uri = prefixes.find(p => x.getURI.startsWith(p.nameSpace)).map(p => TripleItem.PrefixedUri(p.prefix, p.nameSpace, x.getURI.substring(p.nameSpace.length))).getOrElse(TripleItem.LongUri(x.getURI))

    def prefix(prefix: String, iri: String): Unit = prefixes += Prefix(prefix, iri)

    def start(): Unit = {}

    def quad(quad: Quad): Unit = {
      val triple = rdfrules.data.Triple(
        quad.getSubject match {
          case x: Node_URI => uriToTripleItem(x)
          case x: Node_Blank => TripleItem.BlankNode(x.getBlankNodeId.getLabelString)
          case _ => throw new IllegalArgumentException
        },
        quad.getPredicate match {
          case x: Node_URI => uriToTripleItem(x)
          case _ => throw new IllegalArgumentException
        },
        quad.getObject match {
          case x: Node_Literal => x.getLiteralValue match {
            case x: java.lang.Integer => TripleItem.Number(x.intValue())
            case x: java.lang.Double => TripleItem.Number(x.doubleValue())
            case x: java.lang.Short => TripleItem.Number(x.shortValue())
            case x: java.lang.Float => TripleItem.Number(x.floatValue())
            case x: java.lang.Long => TripleItem.Number(x.longValue())
            case x: java.lang.Byte => TripleItem.Number(x.byteValue())
            case x: java.lang.Boolean => TripleItem.BooleanValue(x.booleanValue())
            case x: java.math.BigInteger => TripleItem.Number(x.longValueExact())
            case x: java.math.BigDecimal => TripleItem.Number(x.doubleValue())
            case _ =>
              val text = x.getLiteralLexicalForm
              TripleItem.Interval(text).getOrElse(TripleItem.Text(text))
          }
          case x: Node_URI => uriToTripleItem(x)
          case x: Node_Blank => TripleItem.BlankNode(x.getBlankNodeId.getLabelString)
          case _ => throw new IllegalArgumentException
        }
      )
      f(quad.getGraph match {
        case x: Node_URI => triple.toQuad(uriToTripleItem(x))
        case _ => triple.toQuad
      })
    }

    def triple(triple: graph.Triple): Unit = quad(new Quad(null, triple))

    def finish(): Unit = {}

    def base(base: String): Unit = prefixes += Prefix("", base)
  }

  implicit def jenaLangToRdfReader(jenaLang: Lang): RdfReader = (inputStreamBuilder: InputStreamBuilder) => new Traversable[rdfrules.data.Quad] {
    def foreach[U](f: rdfrules.data.Quad => U): Unit = {
      val is = new BufferedInputStream(inputStreamBuilder.build)
      try {
        RDFDataMgr.parse(new StreamRdfImpl(f), is, jenaLang)
      } finally {
        is.close()
      }
    }
  }.view

  implicit def jenaFormatToRdfWriter(rdfFormat: RDFFormat): RdfWriter = (quads: QuadTraversableView, outputStreamBuilder: OutputStreamBuilder) => {
    val os = new BufferedOutputStream(outputStreamBuilder.build)
    val stream = StreamRDFWriter.getWriterStream(os, rdfFormat)
    try {
      stream.start()
      for (prefix <- quads.prefixes) {
        stream.prefix(prefix.prefix, prefix.nameSpace)
      }
      rdfFormat.getLang match {
        case RDFLanguages.N3 | RDFLanguages.NT | RDFLanguages.NTRIPLES | RDFLanguages.TTL | RDFLanguages.TURTLE =>
          quads.foreach(quad => stream.triple(quad.triple))
        case _ =>
          quads.foreach(quad => stream.quad(quad))
      }
    } finally {
      stream.finish()
      os.close()
    }
  }

}