package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.github.propi.rdfrules.gui.results.Quads.Quad
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Quads(val title: String, val id: Future[String]) extends ActionProgress with Pagination[Quad] {

  @dom
  def viewRecord(record: Quad): Binding[Div] = <div class="quad">
    <div class="subject">
      {record.subject}
    </div>
    <div class="predicate">
      {record.predicate}
    </div>
    <div class="object">
      {record.`object`.toString}
    </div>
    <div class="graph">
      {record.graph}
    </div>
  </div>

  @dom
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="quads">
    <div class="quads-amount">
      <span class="text">Number of quads:</span>
      <span class="number">
        {result.value.size.toString}
      </span>
    </div>
    <div class="quads-body">
      {viewRecords(result.value.view.map(_.asInstanceOf[Quad])).bind}
    </div>
    <div class="rules-pages">
      {viewPages(result.value.size).bind}
    </div>
  </div>

}

object Quads {

  trait Quad extends js.Object {
    val subject: String
    val predicate: String
    val `object`: js.Dynamic
    val graph: String
  }

}