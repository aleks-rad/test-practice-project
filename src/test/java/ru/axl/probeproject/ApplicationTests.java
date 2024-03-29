package ru.axl.probeproject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.axl.probeproject.controllers.AccountController;
import ru.axl.probeproject.controllers.AccountStatusController;
import ru.axl.probeproject.controllers.ClientController;
import ru.axl.probeproject.controllers.DictionaryController;
import ru.axl.probeproject.controllers.ProcessController;
import ru.axl.probeproject.controllers.ProcessStatusController;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationTests {

	@Autowired
	private ClientController clientController;
	@Autowired
	private ProcessController processController;
	@Autowired
	private ProcessStatusController processStatusController;
	@Autowired
	private AccountController accountController;
	@Autowired
	private AccountStatusController accountStatusController;
	@Autowired
	private DictionaryController dictionaryController;

	@Test
	void contextLoads() {
		assertThat(clientController).isNotNull();
		assertThat(processController).isNotNull();
		assertThat(processStatusController).isNotNull();
		assertThat(accountController).isNotNull();
		assertThat(accountStatusController).isNotNull();
		assertThat(dictionaryController).isNotNull();
	}

}
