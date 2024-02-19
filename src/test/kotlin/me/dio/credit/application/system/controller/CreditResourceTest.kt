package me.dio.credit.application.system.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.dio.credit.application.system.dto.request.CreditDto
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.repository.CustomerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CreditResourceTest {
  @Autowired
  private lateinit var customerRepository: CustomerRepository

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  companion object {
    const val URL: String = "/api/credits"
  }

  @BeforeEach
  fun setup() = customerRepository.deleteAll()

  @AfterEach
  fun tearDown() = customerRepository.deleteAll()

  @Test
  fun `should create a credit and return 201 status`() {
    //given
    val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
    val creditDto: CreditDto = buildCreditDto(customerId = customer.id!!)
    val valueAsString: String = objectMapper.writeValueAsString(creditDto)
    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.post(URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(valueAsString)
    )
      .andExpect(MockMvcResultMatchers.status().isCreated)
      .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value(100.0))
      .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value(15))
      .andExpect(MockMvcResultMatchers.jsonPath("$.dayFirstOfInstallment").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_PROGRESS"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value("camila@email.com"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value(1000.00))
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should not save a credit with invalid numberOfInstallment and return 400 status`() {
    //given
    val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
    val creditDto: CreditDto = buildCreditDto(
      numberOfInstallments = 49,
      customerId = customer.id!!)
    val valueAsString: String = objectMapper.writeValueAsString(creditDto)
    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.post(URL)
        .content(valueAsString)
        .contentType(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isBadRequest)
      .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
      .andExpect(
        MockMvcResultMatchers.jsonPath("$.exception")
          .value("class org.springframework.web.bind.MethodArgumentNotValidException")
      )
      .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should not save a credit with invalid dayFirstOfInstallment and return 400 status`() {
    //given
    val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
    val creditDto: CreditDto = buildCreditDto(
      dayFirstOfInstallment = LocalDate.now().plusMonths(4L),
      customerId = customer.id!!)
    val valueAsString: String = objectMapper.writeValueAsString(creditDto)
    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.post(URL)
        .content(valueAsString)
        .contentType(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isBadRequest)
      .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
      .andExpect(
        MockMvcResultMatchers.jsonPath("$.exception")
          .value("class me.dio.credit.application.system.exception.BusinessException")
      )
      .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should find all credits by customer id and return 200 status`() {
    //given
    val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
    val credit1: Credit = creditRepository.save(buildCredit(customer = customer))
    val credit2: Credit = creditRepository.save(buildCredit(customer = customer))
    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.get("$URL?customerId=${customer.id}")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect(MockMvcResultMatchers.jsonPath("$.[*]").isNotEmpty)
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should not find any credits by customer id and return 200 status`() {
    //given
    val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.get("$URL?customerId=${customer.id}")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect(MockMvcResultMatchers.jsonPath("$.[*]").isEmpty)
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should not find any credits by non-existent customer id and return 200 status`() {
    //given
    val invalidId: Long = Random().nextLong()
    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.get("$URL?customerId=$invalidId")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect(MockMvcResultMatchers.jsonPath("$.[*]").isEmpty)
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should find credit by creditCode and customer id and return 200 status`() {
    //given
    val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
    val credit: Credit = creditRepository.save(buildCredit(customer = customer))
    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.get("$URL/${credit.creditCode}?customerId=${customer.id}")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value(100.0))
      .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value(15))
      .andExpect(MockMvcResultMatchers.jsonPath("$.dayFirstOfInstallment").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_PROGRESS"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value("camila@email.com"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value(1000.00))
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should not find credit by invalid creditCode and customer id and return 400 status`() {
    //given
    val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.get("$URL/${UUID.randomUUID()}?customerId=${customer.id}")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isBadRequest)
      .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
      .andExpect(
        MockMvcResultMatchers.jsonPath("$.exception")
          .value("class me.dio.credit.application.system.exception.BusinessException")
      )
      .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
      .andDo(MockMvcResultHandlers.print())
  }

  @Test
  fun `should not find credit by creditCode and invalid customer id and return 400 status`() {
    //given
    val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
    val credit: Credit = creditRepository.save(buildCredit(customer = customer))
    val invalidId: Long = customer.id!! + 1L
    //when
    //then
    mockMvc.perform(
      MockMvcRequestBuilders.get("$URL/${credit.creditCode}?customerId=$invalidId")
        .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(MockMvcResultMatchers.status().isBadRequest)
      .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
      .andExpect(
        MockMvcResultMatchers.jsonPath("$.exception")
          .value("class java.lang.IllegalArgumentException")
      )
      .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
      .andDo(MockMvcResultHandlers.print())
  }

  private fun buildCreditDto(
    creditValue: BigDecimal = BigDecimal.valueOf(100.0),
    dayFirstOfInstallment: LocalDate = LocalDate.now().plusMonths(2L),
    numberOfInstallments: Int = 15,
    customerId: Long
  ): CreditDto = CreditDto(
    creditValue = creditValue,
    dayFirstOfInstallment = dayFirstOfInstallment,
    numberOfInstallments = numberOfInstallments,
    customerId = customerId
  )

  private fun buildCredit(
    creditValue: BigDecimal = BigDecimal.valueOf(100.0),
    dayFirstInstallment: LocalDate = LocalDate.now().plusMonths(2L),
    numberOfInstallments: Int = 15,
    customer: Customer
  ): Credit = Credit(
    creditValue = creditValue,
    dayFirstInstallment = dayFirstInstallment,
    numberOfInstallments = numberOfInstallments,
    customer = customer
  )

  private fun builderCustomerDto(
    firstName: String = "Cami",
    lastName: String = "Cavalcante",
    cpf: String = "28475934625",
    email: String = "camila@email.com",
    income: BigDecimal = BigDecimal.valueOf(1000.0),
    password: String = "1234",
    zipCode: String = "000000",
    street: String = "Rua da Cami, 123",
  ) = CustomerDto(
    firstName = firstName,
    lastName = lastName,
    cpf = cpf,
    email = email,
    income = income,
    password = password,
    zipCode = zipCode,
    street = street
  )
}