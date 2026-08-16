package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BeginnerGuideRequestServiceTest {

    @Mock private BeginnerGuideRepository guideRepository;
    @Mock private BeginnerGuideGenerationQuota quota;
    @Mock private BeginnerGuideGenerationService generationService;
    @InjectMocks private BeginnerGuideRequestService service;

    @Test
    void shouldReturnExistingGuideWithoutCheckingQuota() {
        var existing = org.mockito.Mockito.mock(BeginnerGuide.class);
        when(guideRepository.findById(42L)).thenReturn(Optional.of(existing));

        var result = service.request(42L);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(quota, generationService);
    }

    @Test
    void shouldGenerateWithQuotaRequesterWhenGuideIsAbsent() {
        var generated = org.mockito.Mockito.mock(BeginnerGuide.class);
        when(guideRepository.findById(42L)).thenReturn(Optional.empty());
        when(quota.requireAvailable()).thenReturn("user-1");
        when(generationService.generate(42L, "user-1")).thenReturn(generated);

        var result = service.request(42L);

        assertThat(result).isSameAs(generated);
        verify(quota).requireAvailable();
        verify(generationService).generate(42L, "user-1");
    }
}
