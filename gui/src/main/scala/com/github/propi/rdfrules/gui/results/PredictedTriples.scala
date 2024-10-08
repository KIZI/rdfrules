package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.components.Multiselect
import com.github.propi.rdfrules.gui.results.InstantiatedRules.InstantiatedAtom
import com.github.propi.rdfrules.gui.results.PredictedTriples.{PredictedTriple, viewPredictedResultMark, viewPredictedTripleScore, viewTriple}
import com.github.propi.rdfrules.gui.results.Rules._
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.{Constants, Var}
import org.lrng.binding.html
import org.scalajs.dom.Event
import org.scalajs.dom.html.{Div, LI}
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class PredictedTriples(val title: String, val id: Future[String], showRules: Boolean) extends ActionProgress with Pagination[(PredictedTriple, Int)] {

  private val withRules: Var[Boolean] = Var(showRules)
  private var lastWithRules: Boolean = showRules
  private val fulltext: Var[String] = Var("")
  private var lastFulltext: String = ""
  private var lastResult: Option[IndexedSeq[js.Dynamic]] = None
  private val predictedResultsSelection = new Multiselect(Constants(
    PredictedResult.Positive.toString -> PredictedResult.Positive.label,
    PredictedResult.Negative.toString -> PredictedResult.Negative.label,
    PredictedResult.PcaPositive.toString -> PredictedResult.PcaPositive.label
  ), placeholder = "Predicted head")
  private val predictedResults: Binding[Set[PredictedResult]] = predictedResultsSelection.selectedValuesBinding.all.map(_.iterator.flatMap(PredictedResult(_)).toSet)
  private var lastPredictedResults: Set[PredictedResult] = Set.empty

  private def exportJson(triples: Seq[js.Dynamic]): Unit = {
    Downloader.download("prediction.json", js.Array(triples: _*))
  }

  private def exportRdf(triples: Seq[js.Dynamic]): Unit = {
    val textRules = triples.view.map(_.asInstanceOf[PredictedTriple]).map { triple =>
      s"${Rules.viewAtomItem(triple.triple.subject)} ${Rules.viewAtomItem(triple.triple.predicate)} ${Rules.viewAtomItem(triple.triple.`object`)} ."
    }
    Downloader.download("prediction.nt", textRules)
  }

  private def filterRulesByPredictedResults(rules: Iterable[js.Dynamic], predictedResults: Set[PredictedResult]): Iterable[js.Dynamic] = {
    if (predictedResults.isEmpty) {
      rules
    } else {
      rules.view.filter(x => PredictedResult(x.asInstanceOf[PredictedTriple].predictedResult).forall(predictedResults))
    }
  }

  private def filterRulesByFulltext(rules: Iterable[js.Dynamic], text: String, withRules: Boolean): Iterable[js.Dynamic] = if (text.isEmpty) {
    rules
  } else {
    val normFulltext = Globals.stripText(text).toLowerCase
    rules.view.filter { x =>
      val rule = x.asInstanceOf[PredictedTriple]
      val rules = if (withRules) {
        rule.rules.iterator
          .flatMap(rule => rule.body.iterator ++ Iterator(rule.head))
          .flatMap(x => Iterator(viewAtomItem(x.subject), viewAtomItem(x.predicate), viewAtomItem(x.`object`)) ++ x.graphs.toOption.iterator.flatten.map(viewAtomItem))
      } else {
        Iterator.empty
      }
      val triple = Iterator(Rules.viewAtomItem(rule.triple.subject), Rules.viewAtomItem(rule.triple.predicate), Rules.viewAtomItem(rule.triple.`object`))
      (rules ++ triple)
        .map(Globals.stripText(_).toLowerCase)
        .exists(_.contains(normFulltext))
    }
  }

  private def filteredTriples(rules: Iterable[js.Dynamic], fulltext: String, predictedResults: Set[PredictedResult], withRules: Boolean): IndexedSeq[js.Dynamic] = {
    if (fulltext != lastFulltext || predictedResults != lastPredictedResults || withRules != lastWithRules) lastResult = None
    lastResult match {
      case Some(x) => x
      case None =>
        val _lastResult = filterRulesByFulltext(filterRulesByPredictedResults(rules, predictedResults), fulltext, withRules).toVector
        lastResult = Some(_lastResult)
        lastFulltext = fulltext
        lastPredictedResults = predictedResults
        lastWithRules = withRules
        _lastResult
    }
  }

  @html
  def viewRule(rule: Rule): Binding[LI] = <li class="rule">
    <div class="text">
      <span>
        {rule.body.map(viewAtom).mkString(" ^ ")}
      </span>
      <span>
        &rArr;
      </span>
      <span>
        {viewAtom(rule.head)}
      </span>
    </div>
    <div class="measures">
      {rule.measures.mapApproximativeMeasures.map(x => s"${x.name}: ${x.value}").mkString(", ")}
    </div>
  </li>

  @html
  def viewRecord(record: (PredictedTriple, Int)): Binding[Div] = <div class="predicted-triple">
    <div class="record">
      <div class="num">
        {(record._2 + 1).toString + ":"}
      </div>{viewPredictedResultMark(record._1.predictedResult).bind}{viewTriple(record._1.triple.subject, record._1.triple.predicate, record._1.triple.`object`).bind}{viewPredictedTripleScore(record._1.score).bind}
    </div>{if (withRules.bind) {
      val n = Var(3)
      <ul class="rules">
        {for (rule <- Constants(record._1.rules.iterator.take(n.bind).toSeq: _*)) yield {
        viewRule(rule).bind
      }}{if (record._1.rules.length > n.bind) {
        <li class="more">
          <a href="#" onclick={e: Event =>
            e.preventDefault()
            n.value = n.value + 5}>show more</a>
        </li>
      } else {
        <!-- empty content -->
      }}
      </ul>
    } else {
      <!-- empty content -->
    }}
  </div>

  @html
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div class="predicted-triples">
    <div class="rules-amount">
      <span class="text">Number of predicted triples:</span>
      <span class="number">
        {filteredTriples(result.value, fulltext.bind, predictedResults.bind, withRules.bind).size.toString}
      </span>
    </div>
    <div class="rules-tools">
      <a href="#" onclick={e: Event =>
        e.preventDefault()
        exportJson(filteredTriples(result.value, fulltext.value, predictedResultsSelection.selectedValues.flatMap(PredictedResult(_)), withRules.value))}>Export as JSON</a>
      <a href="#" onclick={e: Event =>
        e.preventDefault()
        exportRdf(filteredTriples(result.value, fulltext.value, predictedResultsSelection.selectedValues.flatMap(PredictedResult(_)), withRules.value))}>Export as RDF</a>
      <input type="text" class="fulltext" value={fulltext.bind} onkeyup={e: Event =>
        fulltext.value = e.target.asInstanceOf[HTMLInputElement].value
        setPage(1)}></input>{predictedResultsSelection.view.bind}<label>show rules:
      <input type="checkbox" checked={withRules.bind} onchange={e: Event =>
      withRules.value = e.target.asInstanceOf[HTMLInputElement].checked}/>
    </label>
    </div>
    <div class="rules-body">
      {viewRecords(filteredTriples(result.value, fulltext.bind, predictedResults.bind, withRules.bind).view.map(_.asInstanceOf[PredictedTriple]).zipWithIndex).bind}
    </div>
    <div class="rules-pages">
      {viewPages(filteredTriples(result.value, fulltext.bind, predictedResults.bind, withRules.bind).size).bind}
    </div>
  </div>

}

object PredictedTriples {

  trait PredictedTriple extends js.Object {
    val triple: InstantiatedAtom
    val rules: js.Array[Rule]
    val predictedResult: String
    val score: Double
  }

  @html
  def viewPredictedResultMark(predictedResult: String): Binding[Div] = <div class={s"predicted-result ${predictedResult.toLowerCase}"} title={PredictedResult(predictedResult).map(_.label).getOrElse("")}>
    {PredictedResult(predictedResult).map(_.symbol).getOrElse("!")}
  </div>

  @html
  def viewPredictedTripleScore(score: Double): Binding[Div] = <div class="predicted-triple-score">
    {if (score == 0.0) "" else s"Score: $score"}
  </div>

  @html
  def viewTriple(s: js.Dynamic, p: js.Dynamic, o: js.Dynamic): Binding[Div] = <div class="quad">
    <div class="subject">
      {Rules.viewAtomItem(s)}
    </div>
    <div class="predicate">
      {Rules.viewAtomItem(p)}
    </div>
    <div class="object">
      {Rules.viewAtomItem(o)}
    </div>
  </div>

}