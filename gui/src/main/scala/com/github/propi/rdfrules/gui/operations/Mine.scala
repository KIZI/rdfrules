package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, LowerThanOrEqualsTo, NonEmpty, RegExp}
import com.github.propi.rdfrules.gui.utils.ReactiveBinding
import com.github.propi.rdfrules.gui.utils.ReactiveBinding.PimpedBindingSeq
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constant, Constants, Var}
import org.lrng.binding.html
import org.scalajs.dom.html.{Div, Span}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Mine(fromOperation: Operation, val info: OperationInfo) extends Operation {

  private object RuleConsumers extends Property.FixedProps with Property.FixedHidden {
    val name: Constant[String] = Constant("ruleConsumers")
    val title: Constant[String] = Constant("Rule consumer")
    val description: Var[String] = Var(context(title.value).description)
    val summaryTitle: SummaryTitle = SummaryTitle.Empty
    val isHidden: Constant[Boolean] = Constant(false)

    private var hasTopK: Boolean = false
    private var hasOnDisk: Boolean = false
    private var selectedFormat: String = "ndjson"

    private val properties = context.use(title.value) { implicit context =>
      val (k, allowOverflow) = context.use("Top-k") { implicit context =>
        val k = new DynamicElement(Constants(new FixedText[Double]("k", "k-value", validator = GreaterThanOrEqualsTo[Int](1), summaryTitle = "top")), hidden = true)
        val allowOverflow = new DynamicElement(Constants(new Checkbox("allowOverflow", "Allow overflow")), hidden = true)
        k -> allowOverflow
      }
      val (file, format) = context.use("On disk") { implicit context =>
        val file = new DynamicElement(Constants(new ChooseFileFromWorkspace(Workspace.loadFiles, true, "file", "Export path", validator = NonEmpty)), hidden = true)
        val format = new DynamicElement(Constants(new Select("format", "Export rules format", Constants("txt" -> "Text (unparsable)", "ndjson" -> "streaming NDJSON (as model - parsable)"), Some(selectedFormat), (value, _) => selectedFormat = value)), hidden = true)
        file -> format
      }
      Constants(
        new Checkbox("topk", "Top-k", onChecked = { isChecked =>
          hasTopK = isChecked
          if (isChecked) {
            k.setElement(0)
            allowOverflow.setElement(0)
          } else {
            k.setElement(-1)
            allowOverflow.setElement(-1)
          }
        }),
        k,
        allowOverflow,
        new Checkbox("onDisk", "On disk", onChecked = { isChecked =>
          hasOnDisk = isChecked
          if (isChecked) {
            file.setElement(0)
            format.setElement(0)
          } else {
            file.setElement(-1)
            format.setElement(-1)
          }
        }),
        file,
        format
      )
    }

    private val summaryProperty = properties.findBinding(_.hasSummary)

    override def hasSummary: Binding[Boolean] = summaryProperty.map(_.nonEmpty)

    def summaryContentView: Binding[Span] = summaryProperty.flatMap(_.map(_.summaryView).getOrElse(ReactiveBinding.emptySpan))

    override def summaryView: Binding[Span] = summaryContentView

    @html
    def valueView: Binding[Div] = <div class="properties sub">
      <table>
        {for (property <- properties) yield {
        property.view.bind
      }}
      </table>
    </div>

    def validate(): Option[String] = properties.value.iterator.map(_.validate()).find(_.nonEmpty).flatten.map(x => s"There is an error within '${title.value}' properties: $x")

    def setValue(data: js.Dynamic): Unit = {
      for (x <- data.asInstanceOf[js.Array[js.Dynamic]]) {
        if (x.name.asInstanceOf[String] == "topK") {
          properties.value.head.setValue(js.Any.fromBoolean(true).asInstanceOf[js.Dynamic])
          properties.value(1).setValue(x.k)
          properties.value(2).setValue(x.allowOverflow)
        }
        if (x.name.asInstanceOf[String] == "onDisk") {
          properties.value(3).setValue(js.Any.fromBoolean(true).asInstanceOf[js.Dynamic])
          properties.value(4).setValue(x.file)
          properties.value(5).setValue(x.format)
        }
      }
    }

    def toJson: js.Any = {
      val consumers = js.Array[js.Any]()
      if (hasTopK) {
        consumers.push(js.Dictionary("name" -> "topK", properties.value(1).getName -> properties.value(1).toJson, properties.value(2).getName -> properties.value(2).toJson))
      } else if (!hasOnDisk || selectedFormat != "ndjson") {
        consumers.push(js.Dictionary("name" -> "inMemory"))
      }
      if (hasOnDisk) {
        consumers.push(js.Dictionary("name" -> "onDisk", properties.value(4).getName -> properties.value(4).toJson, properties.value(5).getName -> properties.value(5).toJson))
      }
      consumers
    }
  }

  val properties: Constants[Property] = {
    val thresholds = DynamicGroup("thresholds", "Thresholds", SummaryTitle.NoTitle) { implicit context =>
      val summaryTitle = SummaryTitle.Variable(Var(""))
      val value1 = new DynamicElement(Constants(
        context.use("MinHeadCoverage")(implicit context => new FixedText[Double]("value", "Value", "0.1", GreaterThanOrEqualsTo(0.001).map[String] & LowerThanOrEqualsTo(1.0).map[String], summaryTitle)),
        context.use("MinHeadSize or MinSupport or Timeout")(implicit context => new FixedText[Double]("value", "Value", validator = GreaterThanOrEqualsTo[Int](1), summaryTitle = summaryTitle)),
        context.use("MaxRuleLength")(implicit context => new FixedText[Double]("value", "Value", validator = GreaterThanOrEqualsTo[Int](2), summaryTitle = summaryTitle)),
        context.use("MinAtomSize")(implicit context => new FixedText[Double]("value", "Value", default = "-1", summaryTitle = summaryTitle)),
        context.use("LocalTimeout")(implicit context => new FixedText[Double]("value", "Refinement timeout (ms)", "1000", GreaterThanOrEqualsTo[Int](0), "Refinement timeout"))
      ), true)
      val value2 = new DynamicElement(Constants(
        context.use("LocalTimeout")(implicit context => new FixedText[Double]("me", "Margin of error", "0", GreaterThanOrEqualsTo(0.0).map[String] & LowerThanOrEqualsTo(1.0).map[String], "Margin of error"))
      ), true)
      val value3 = new DynamicElement(Constants(
        context.use("LocalTimeout")(implicit context => new Checkbox("dme", "Dynamic margin of error", summaryTitle = "DME"))
      ), true)

      def activateParams(num: Int, other: Boolean): Unit = {
        value1.setElement(num)
        val otherIndex = if (other) 0 else -1
        value2.setElement(otherIndex)
        value3.setElement(otherIndex)
      }

      Constants(
        new Select(
          "name",
          "Name",
          Constants("MinHeadSize" -> "Min head size", "MinAtomSize" -> "Min atom size", "MinHeadCoverage" -> "Min head coverage", "MinSupport" -> "Min support", "MaxRuleLength" -> "Max rule length", "Timeout" -> "Timeout", "LocalTimeout" -> "Rule refinement timeout and/or sampling"),
          onSelect = (selectedItem, selectedValue) => {
            summaryTitle.title.value = selectedValue
            selectedItem match {
              case "MinHeadCoverage" => activateParams(0, false)
              case "MinHeadSize" | "MinSupport" | "Timeout" => activateParams(1, false)
              case "MaxRuleLength" => activateParams(2, false)
              case "MinAtomSize" => activateParams(3, false)
              case "LocalTimeout" => activateParams(4, true)
              case _ => activateParams(-1, false)
            }
          }
        ),
        value1,
        value2,
        value3
      )
    }
    val constraints = DynamicGroup("constraints", "Constraints", SummaryTitle.NoTitle) { implicit context =>
      val value = new DynamicElement(Constants(
        context.use("OnlyPredicates or WithoutPredicates")(implicit context => ArrayElement("values", "Values")(implicit context => new OptionalText[String]("value", "Value", validator = RegExp("<.*>|\\w+:.*")))),
        context.use("ConstantsForPredicatePosition")(implicit context => DynamicGroup("values", "Constant position for predicates") { implicit context =>
          Constants(
            new FixedText[String]("predicate", "Predicate URI", validator = RegExp("<.*>|\\w+:.*")),
            new Select("position", "Constant position", Constants("Subject" -> "Subject", "Object" -> "Object", "LowerCardinalitySide" -> "Lower cardinality side", "Both" -> "Both"), summaryTitle = SummaryTitle.NoTitle)
          )
        })
      ), true)
      Constants(
        new Select("name", "Name", Constants("WithoutConstants" -> "Without constants", "OnlyObjectConstants" -> "With constants at the object position", "OnlySubjectConstants" -> "With constants at the subject position", "OnlyLowerCardinalitySideConstants" -> "With constants at the lower cardinality side", "OnlyLowerCardinalitySideAtHeadConstants" -> "With constants at the lower cardinality side only in the head", "ConstantsForPredicates" -> "With constants for selected predicates", "WithoutDuplicitPredicates" -> "Without duplicit predicates", "OnlyPredicates" -> "Only predicates", "WithoutPredicates" -> "Without predicates"), onSelect = {
          case ("OnlyPredicates", _) | ("WithoutPredicates", _) => value.setElement(0)
          case ("ConstantsForPredicates", _) => value.setElement(1)
          case _ => value.setElement(-1)
        }, summaryTitle = SummaryTitle.NoTitle),
        value
      )
    }
    thresholds.setValue(js.Array(
      js.Dictionary("name" -> "MinHeadSize", "value" -> 100),
      js.Dictionary("name" -> "MinHeadCoverage", "value" -> 0.01),
      js.Dictionary("name" -> "MaxRuleLength", "value" -> 3),
      js.Dictionary("name" -> "Timeout", "value" -> 5)
    ).asInstanceOf[js.Dynamic])
    constraints.setValue(js.Array(js.Dictionary("name" -> "WithoutConstants")).asInstanceOf[js.Dynamic])
    Constants(
      thresholds,
      RuleConsumers,
      Pattern("patterns", "Patterns", false),
      constraints,
      new FixedText[Int]("parallelism", "Parallelism", "0", GreaterThanOrEqualsTo[Int](0))
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}