package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.RedirectRule;
import com.mycompany.myapp.repository.RedirectRuleRepository;
import com.mycompany.myapp.repository.search.RedirectRuleSearchRepository;
import com.mycompany.myapp.service.RedirectRuleService;
import com.mycompany.myapp.service.dto.RedirectRuleDTO;
import com.mycompany.myapp.service.mapper.RedirectRuleMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.RedirectRule}.
 */
@Service
@Transactional
public class RedirectRuleServiceImpl implements RedirectRuleService {

    private static final Logger LOG = LoggerFactory.getLogger(RedirectRuleServiceImpl.class);

    private final RedirectRuleRepository redirectRuleRepository;

    private final RedirectRuleMapper redirectRuleMapper;

    private final RedirectRuleSearchRepository redirectRuleSearchRepository;

    public RedirectRuleServiceImpl(
        RedirectRuleRepository redirectRuleRepository,
        RedirectRuleMapper redirectRuleMapper,
        RedirectRuleSearchRepository redirectRuleSearchRepository
    ) {
        this.redirectRuleRepository = redirectRuleRepository;
        this.redirectRuleMapper = redirectRuleMapper;
        this.redirectRuleSearchRepository = redirectRuleSearchRepository;
    }

    @Override
    public RedirectRuleDTO save(RedirectRuleDTO redirectRuleDTO) {
        LOG.debug("Request to save RedirectRule : {}", redirectRuleDTO);
        RedirectRule redirectRule = redirectRuleMapper.toEntity(redirectRuleDTO);
        redirectRule = redirectRuleRepository.save(redirectRule);
        redirectRuleSearchRepository.index(redirectRule);
        return redirectRuleMapper.toDto(redirectRule);
    }

    @Override
    public RedirectRuleDTO update(RedirectRuleDTO redirectRuleDTO) {
        LOG.debug("Request to update RedirectRule : {}", redirectRuleDTO);
        RedirectRule redirectRule = redirectRuleMapper.toEntity(redirectRuleDTO);
        redirectRule = redirectRuleRepository.save(redirectRule);
        redirectRuleSearchRepository.index(redirectRule);
        return redirectRuleMapper.toDto(redirectRule);
    }

    @Override
    public Optional<RedirectRuleDTO> partialUpdate(RedirectRuleDTO redirectRuleDTO) {
        LOG.debug("Request to partially update RedirectRule : {}", redirectRuleDTO);

        return redirectRuleRepository
            .findById(redirectRuleDTO.getId())
            .map(existingRedirectRule -> {
                redirectRuleMapper.partialUpdate(existingRedirectRule, redirectRuleDTO);

                return existingRedirectRule;
            })
            .map(redirectRuleRepository::save)
            .map(savedRedirectRule -> {
                redirectRuleSearchRepository.index(savedRedirectRule);
                return savedRedirectRule;
            })
            .map(redirectRuleMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RedirectRuleDTO> findOne(Long id) {
        LOG.debug("Request to get RedirectRule : {}", id);
        return redirectRuleRepository.findById(id).map(redirectRuleMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete RedirectRule : {}", id);
        redirectRuleRepository.deleteById(id);
        redirectRuleSearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RedirectRuleDTO> search(String query, Pageable pageable) {
        LOG.debug("Request to search for a page of RedirectRules for query {}", query);
        return redirectRuleSearchRepository.search(query, pageable).map(redirectRuleMapper::toDto);
    }
}
