package ru.axl.probeproject.services.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.axl.probeproject.exceptions.ApiException;
import ru.axl.probeproject.mapper.ProcessMapper;
import ru.axl.probeproject.model.ProcessRequest;
import ru.axl.probeproject.model.ProcessResponse;
import ru.axl.probeproject.model.entities.Client;
import ru.axl.probeproject.model.entities.Process;
import ru.axl.probeproject.model.entities.ProcessStatus;
import ru.axl.probeproject.repositories.ClientRepository;
import ru.axl.probeproject.repositories.ProcessRepository;
import ru.axl.probeproject.repositories.ProcessStatusRepository;
import ru.axl.probeproject.services.ProcessService;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ru.axl.probeproject.exceptions.ApiError.*;
import static ru.axl.probeproject.model.enums.ProcessStatusEnum.*;
import static ru.axl.probeproject.utils.Utils.getNowOffsetDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class ProcessServiceImpl implements ProcessService {

    private final ProcessRepository processRepo;
    private final ClientRepository clientRepo;
    private final ProcessStatusRepository processStatusRepo;
    private final ProcessMapper processMapper;

    private final Set<String> notTerminalStatuses = Set.of(
        NEW.name(), COMPLIANCE_SUCCESS.name(), COMPLIANCE_ERROR.name(), OPENING.name()
    );

    @Override
    @Transactional
    public ProcessResponse createProcess(ProcessRequest processRequest) {
        log.info("Создание нового процесса для клиента с инн = {}", processRequest.getClientInn());

        Client client = clientRepo.findByInn(processRequest.getClientInn()).orElseThrow(() ->
                new ApiException(CLIENT_NOT_FOUND,
                        String.format("Не найден клиент с инн = %s", processRequest.getClientInn())));

        List<Process> notTerminalProcesses = processRepo.findAllByIdClientInStatuses(client.getIdClient(),
                notTerminalStatuses);
        if(!notTerminalProcesses.isEmpty()){
            throw new ApiException(CLIENT_HAS_NOT_TERMINATED_PROCESSES,
                    String.format("У клиента есть незавершенные процессы:\n %s", notTerminalProcesses));
        }

        ProcessStatus processStatusNew = processStatusRepo.findByName(NEW.name()).orElseThrow(() ->
                new ApiException(PROCESS_STATUS_NOT_FOUND,
                        String.format("Не найден статус процесса с name = '%s'", NEW.name())));

        Process process = new Process();
        process.setClient(client);
        process.setStartDate(getNowOffsetDateTime());
        process.setLastUpdateDate(getNowOffsetDateTime());
        process.setProcessStatus(processStatusNew);
        process = processRepo.save(process);

        ProcessResponse processResponse = processMapper.toProcessResponse(process);
        log.info("Процесс создан\n {}", processResponse);

        return processResponse;
    }

    @Override
    public List<ProcessResponse> findAllClientProcesses(UUID idClient) {
        log.info("Поиск всех процессов клиента с uuid = {}", idClient);
        List<Process> processes = processRepo.findAllByIdClient(idClient);

        List<ProcessResponse> processResponses = processMapper.toProcessResponseList(processes);
        log.info("Найдены процессы\n {}", processResponses);

        return processResponses;
    }

}
