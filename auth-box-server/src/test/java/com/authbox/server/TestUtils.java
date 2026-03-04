package com.authbox.server;

import com.authbox.base.model.AccessLog;
import com.authbox.base.service.AccessLogService;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class TestUtils {

    private TestUtils() {
        throw new IllegalStateException("Use static methods directly, without using constructor");
    }

    public static void assertLogEntryContains(final AccessLogService accessLogService,
                                              final String... messagesExpected) {
        for (final String message : messagesExpected) {
            internalAssertLogEntryContains(accessLogService, message);
        }
    }

    public static void assertLogEntryContainsAndReset(final AccessLogService accessLogService,
                                                      final String... messagesExpected) {
        for (final String message : messagesExpected) {
            internalAssertLogEntryContains(accessLogService, message);
        }
        reset(accessLogService);
    }

    private static void internalAssertLogEntryContains(final AccessLogService accessLogService,
                                                       final String messageExpected) {
        val captorBuilder = ArgumentCaptor.forClass(AccessLog.AccessLogBuilder.class);
        val captorMessage = ArgumentCaptor.forClass(String.class);
        val captorArguments = ArgumentCaptor.forClass(String[].class);
        verify(accessLogService, atLeast(1)).create(
                captorBuilder.capture(),
                captorMessage.capture(),
                captorArguments.capture()
        );
        val valuesBuilders = captorBuilder.getAllValues();
        val valuesMessages = captorMessage.getAllValues();
        val valuesArguments = captorArguments.getAllValues();
        assertThat(valuesBuilders).hasSizeGreaterThanOrEqualTo(1);
        assertThat(valuesMessages).hasSizeGreaterThanOrEqualTo(1);
        val availableMessages = new ArrayList<String>();
        val availableResults = new ArrayList<Boolean>();
        for (int j = 0; j < valuesBuilders.size(); j++) {
            val valuesBuilder = valuesBuilders.get(j);
            val valuesMessage = valuesMessages.get(j);
            val builderMessage = valuesBuilder.build().getMessage();
            var formattedMessage = valuesMessage;

            if (valuesArguments.size() >= j) {
                formattedMessage = formattedMessage.formatted((Object[]) valuesArguments.get(j));
            }

            if (isNotBlank(builderMessage)) {
                availableMessages.add(builderMessage);
            }
            if (isNotBlank(formattedMessage)) {
                availableMessages.add(formattedMessage);
            }

            val fail = (builderMessage != null && !builderMessage.contains(messageExpected))
                    || !formattedMessage.contains(messageExpected);
            availableResults.add(fail);
        }

        if (!availableResults.contains(false)) {
            val unused = Assertions.fail("Message '%s' is not found in AccessLog. \nAvailable messages are: %s",
                    messageExpected, availableMessages);
        }
    }
}
