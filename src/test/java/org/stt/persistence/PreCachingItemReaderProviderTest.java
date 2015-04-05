package org.stt.persistence;

import com.google.common.base.Optional;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;

import static org.hamcrest.Matchers.is;

public class PreCachingItemReaderProviderTest {
    @Test
    public void cachedItemsCanBeReadMultipleTimes() {
        // GIVEN
        TimeTrackingItem expected1 = new TimeTrackingItem("first",
                DateTime.now());
        TimeTrackingItem expected2 = new TimeTrackingItem("second",
                DateTime.now());
        final ItemReader readerMock = Mockito.mock(ItemReader.class);
        ItemReaderTestHelper.givenReaderReturns(readerMock, expected1,
                expected2);
        PreCachingItemReaderProvider sut = new PreCachingItemReaderProvider(new ItemReaderProvider() {
            @Override
            public ItemReader provideReader() {
                return readerMock;
            }
        });
        ItemReader cacher = sut.provideReader();

        // WHEN
        // read all items, so the original reader is definitely exhausted
        while (cacher.read().isPresent()) { // NOPMD

        }

        cacher = sut.provideReader();
        // THEN
        Assert.assertThat(cacher.read().get(), is(expected1));
        Assert.assertThat(cacher.read().get(), is(expected2));
        Assert.assertThat(cacher.read(),
                is(Optional.<TimeTrackingItem> absent()));

        IOUtils.closeQuietly(cacher);
    }

}