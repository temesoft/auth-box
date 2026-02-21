package com.authbox.server;

import com.authbox.base.model.AccessLog;
import com.authbox.base.service.AccessLogService;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestUtils {

    private TestUtils() {
    }

    public static void assertLogEntryContains(final AccessLogService accessLogService,
                                              final String... messages) {
        val captorBuilder = ArgumentCaptor.forClass(AccessLog.AccessLogBuilder.class);
        val captorMessage = ArgumentCaptor.forClass(String.class);
        val captorArguments = ArgumentCaptor.forClass(String[].class);
        verify(accessLogService, times(messages.length)).create(
                captorBuilder.capture(),
                captorMessage.capture(),
                captorArguments.capture()
        );
        val valuesBuilder = captorBuilder.getAllValues();
        val valuesMessage = captorMessage.getAllValues();
        val valuesArguments = captorArguments.getAllValues();
        assertThat(valuesBuilder).hasSize(messages.length);
        assertThat(valuesMessage).hasSize(messages.length);
        val availableMessages = new ArrayList<String>();
        for (int i = 0; i < messages.length; i++) {
            val messageExpected = messages[i];
            val builderMessage = valuesBuilder.get(i).build().getMessage();
            var formattedMessage = valuesMessage.get(i);

            if (valuesArguments.size() >= i) {
                formattedMessage = formattedMessage.formatted((Object[]) valuesArguments.get(i));
            }

            if (isNotBlank(builderMessage)) {
                availableMessages.add(builderMessage);
            }
            if (isNotBlank(formattedMessage)) {
                availableMessages.add(formattedMessage);
            }

            val fail = (builderMessage != null && !builderMessage.contains(messageExpected))
                    || !formattedMessage.contains(messageExpected);

            if (fail) {
                val unused = Assertions.fail("Message '%s' is not found in AccessLog. \nAvailable messages are: %s",
                        messageExpected, availableMessages);
            }
        }
    }
}
