package me.dio.credit.application.system.dto.response

import me.dio.credit.application.system.entity.Credit
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class CreditViewList(
  val creditCode: UUID,
  val creditValue: BigDecimal,
  val numberOfInstallments: Int,
  val dayFirstOfInstallment: LocalDate
) {
  constructor(credit: Credit) : this(
    creditCode = credit.creditCode,
    creditValue = credit.creditValue,
    numberOfInstallments = credit.numberOfInstallments,
    dayFirstOfInstallment = credit.dayFirstInstallment
  )
}
