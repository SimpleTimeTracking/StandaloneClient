package org.stt.persistence;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.stt.model.TimeTrackingItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Theories.class)
public class IOUtilTest {

	@Theory
	public void readAllShouldReadAll(
			@TestedOn(ints = { 0, 1, 10, 100 }) int numberOfItems)
			throws IOException {
		// GIVEN

		ItemReader itemReader = mock(ItemReader.class);
		Collection<TimeTrackingItem> expectedItems = new ArrayList<>();
		BDDMyOngoingStubbing<Optional<TimeTrackingItem>> stubbing = given(itemReader
				.read());
		for (int i = 0; i < numberOfItems; i++) {
			Optional<TimeTrackingItem> item = Optional.of(new TimeTrackingItem(
					Integer.toString(i), DateTime.now()));
			expectedItems.add(item.get());
			stubbing = stubbing.willReturn(item);
		}
		stubbing.willReturn(Optional.<TimeTrackingItem> absent());

		// WHEN
		Collection<TimeTrackingItem> result = IOUtil.readAll(itemReader);

		// THEN
		assertThat(result, equalTo(expectedItems));
		verify(itemReader).close();
	}
}
