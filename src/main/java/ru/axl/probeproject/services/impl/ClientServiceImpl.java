package ru.axl.probeproject.services.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.axl.probeproject.exceptions.ApiException;
import ru.axl.probeproject.mapper.ClientMapper;
import ru.axl.probeproject.model.ClientResponse;
import ru.axl.probeproject.model.entities.Client;
import ru.axl.probeproject.repositories.ClientRepository;
import ru.axl.probeproject.services.ClientService;

import java.util.List;
import java.util.Optional;

import static ru.axl.probeproject.exceptions.ApiError.CLIENT_NOT_FOUND;

@Slf4j
@Service
@AllArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepo;
    private final ClientMapper clientMapper;

    @Override
    public List<ClientResponse> findAll() {
        log.info("Получение всех клиентов");

        final List<Client> clients = clientRepo.findAll();
        final List<ClientResponse> clientResponses = clientMapper.toClientResponseList(clients);
        log.info("Найдены клиенты:\n {}", clientResponses);

        return clientResponses;
    }

    @Override
    public ClientResponse findByInn(final String inn) {
        log.info("Поиск клиента по инн = {}", inn);

        final Optional<Client> clientOpt = clientRepo.findByInn(inn);
        final Client client = clientOpt.orElseThrow(() ->
                new ApiException(CLIENT_NOT_FOUND, String.format("Не найден клиент с инн %s", inn)));

        final ClientResponse clientResponse = clientMapper.toClientResponse(client);
        log.info("Найден клиент:\n {}", clientResponse);

        return clientResponse;
    }

}
