package skinny.orm.feature

import org.joda.time.DateTime
import scalikejdbc._
import skinny.PermittedStrongParameters

trait NoIdTimestampsFeature[Entity] extends NoIdCUDFeature[Entity] with TimestampsFeatureBase[Entity] {

  override protected def namedValuesForCreation(strongParameters: PermittedStrongParameters): Seq[(SQLSyntax, Any)] = {
    val additionalValues = timestampValues(strongParameters.params.contains)
    super.namedValuesForCreation(strongParameters) ++ additionalValues
  }

  override def createWithNamedValues(namedValues: (SQLSyntax, Any)*)(implicit s: DBSession = autoSession): Any = {
    val additionalValues = timestampValues(name => namedValues.exists(_._1 == column.field(name)))
    super.createWithNamedValues(namedValues ++ additionalValues: _*)
  }

  override def updateBy(where: SQLSyntax): UpdateOperationBuilder = {
    val builder = super.updateBy(where)
    builder.addAttributeToBeUpdated(column.field(updatedAtFieldName) -> DateTime.now)
    builder
  }

}
