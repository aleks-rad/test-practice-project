package ru.axl.probeproject.services.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.axl.probeproject.exceptions.ApiException;
import ru.axl.probeproject.mapper.AccountMapper;
import ru.axl.probeproject.model.AccountOpenRequest;
import ru.axl.probeproject.model.AccountReserveRequest;
import ru.axl.probeproject.model.AccountResponse;
import ru.axl.probeproject.model.entities.Account;
import ru.axl.probeproject.model.entities.AccountStatus;
import ru.axl.probeproject.model.entities.Client;
import ru.axl.probeproject.model.entities.Currency;
import ru.axl.probeproject.repositories.AccountRepository;
import ru.axl.probeproject.repositories.ClientRepository;
import ru.axl.probeproject.services.AccountService;
import ru.axl.probeproject.services.AccountStatusService;
import ru.axl.probeproject.services.CurrencyService;
import ru.axl.probeproject.services.ProcessService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import static ru.axl.probeproject.exceptions.ApiError.CLIENT_NOT_FOUND;
import static ru.axl.probeproject.exceptions.ApiError.RESERVED_ACCOUNTS_NOT_FOUND;
import static ru.axl.probeproject.model.enums.AccountStatusEnum.OPENING;
import static ru.axl.probeproject.model.enums.AccountStatusEnum.RESERVED;
import static ru.axl.probeproject.model.enums.ProcessStatusEnum.ACCOUNT_PROCESSING;
import static ru.axl.probeproject.model.enums.ProcessStatusEnum.COMPLIANCE_SUCCESS;
import static ru.axl.probeproject.utils.Utils.getNowOffsetDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepo;
    private final ClientRepository clientRepo;
    private final AccountStatusService accountStatusService;
    private final CurrencyService currencyService;
    private final ProcessService processService;
    private final AccountMapper accountMapper;

    @Override
    public List<AccountResponse> findAllClientAccounts(final UUID idClient) {
        log.info("Поиск всех счетов клиента с uuid = {}", idClient);
        final List<Account> accounts = accountRepo.findAllByIdClient(idClient);

        final List<AccountResponse> accountResponses = accountMapper.toAccountResponseList(accounts);
        log.info("Найдены счета\n {}", accountResponses);

        return accountResponses;
    }

    @Override
    @Transactional
    public AccountResponse reserveAccount(final AccountReserveRequest accountRequest) {
        log.info("Резервирование счета:\n {}", accountRequest);
        final UUID idClient = UUID.fromString(accountRequest.getIdClient());
        log.info("Поиск клиента по idClient = {}", idClient);
        final Client client = clientRepo.findByIdClient(idClient).orElseThrow(() ->
                new ApiException(CLIENT_NOT_FOUND, String.format("Не найден клиент с idClient = \"%s\"", idClient)));

        final AccountStatus accountStatusReserve = accountStatusService.findByName(RESERVED.name());

        processService.checkAndGetProcessActiveStatus(client, COMPLIANCE_SUCCESS);

        final Currency currency = currencyService.findByCode(accountRequest.getCode());

        final Account account = new Account();
        account.setNumber(generateReserveAccountNumber(currency));
        account.setCurrency(currency);
        account.setClient(client);
        account.setReservationDate(getNowOffsetDateTime());
        account.setAccountStatus(accountStatusReserve);

        final Account savedAccount = accountRepo.save(account);
        final AccountResponse accountResponse = accountMapper.toAccountResponse(savedAccount);
        log.info("Счет зарезервирован:\n {}", accountResponse);

        return accountResponse;
    }

    @Override
    @Transactional
    public List<AccountResponse> openAccounts(final AccountOpenRequest accountRequest) {
        log.info("Открытие зарезервированных счетов:\n {}", accountRequest);
        final UUID idClient = UUID.fromString(accountRequest.getIdClient());
        log.info("Поиск клиента по idClient = {}", idClient);
        final Client client = clientRepo.findByIdClient(idClient).orElseThrow(() ->
                new ApiException(CLIENT_NOT_FOUND, String.format("Не найден клиент с idClient = \"%s\"", idClient)));

        final AccountStatus accountStatusOpening = accountStatusService.findByName(OPENING.name());

        final List<Account> reservedAccounts = accountRepo.findAllByIdClient(client.getIdClient()).stream()
                .filter(account -> account.getAccountStatus().getName().equals(RESERVED.name()))
                .collect(Collectors.toList());
        if (reservedAccounts.isEmpty()) {
            throw new ApiException(RESERVED_ACCOUNTS_NOT_FOUND, "Зарезервированных счетов не найдено");
        }

        processService.changeProcessStatusByClient(client, COMPLIANCE_SUCCESS, ACCOUNT_PROCESSING);

        reservedAccounts.forEach(account -> {
            account.setAccountStatus(accountStatusOpening);
            account.setOpeningDate(getNowOffsetDateTime());
        });
        accountRepo.saveAll(reservedAccounts);

        final List<AccountResponse> accountResponses = accountMapper.toAccountResponseList(reservedAccounts);
        log.info("Счета отправленные на открытие:\n {}", accountResponses);

        return accountResponses;
    }

    private String generateReserveAccountNumber(final Currency currency) {
        return String.format("40702%sXXXXXXXXXXXX", currency.getCode());
    }

}
