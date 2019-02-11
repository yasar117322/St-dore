package com.nytimes.android.external.fs3


import com.google.common.base.Charsets.UTF_8
import com.google.gson.Gson
import com.nytimes.android.external.store3.base.Fetcher
import com.nytimes.android.external.store3.base.impl.BarCode
import com.nytimes.android.external.store3.base.impl.StoreBuilder
import com.nytimes.android.external.store3.middleware.GsonSourceParser
import io.reactivex.Maybe
import io.reactivex.Single
import okio.BufferedSource
import okio.Okio
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.io.ByteArrayInputStream

class SourceDiskDaoStoreTest {
    @Mock
    lateinit var fetcher: Fetcher<BufferedSource, BarCode>
    @Mock
    lateinit var diskDAO: SourcePersister
    private val barCode = BarCode("value", KEY)

    @Test
    fun fetcherOnlyCalledOnce() {
        MockitoAnnotations.initMocks(this)
        val parser = GsonSourceParser<Foo>(Gson(), Foo::class.java)
        val store = StoreBuilder.parsedWithKey<BarCode, BufferedSource, Foo>()
                .persister(diskDAO)
                .fetcher(fetcher)
                .parser(parser)
                .open()

        val foo = Foo()
        foo.bar = barCode.getKey()

        val sourceData = Gson().toJson(foo)
        val source = source(sourceData)
        val value = Single.just(source)
        `when`(fetcher.fetch(barCode))
                .thenReturn(value)

        `when`(diskDAO.read(barCode))
                .thenReturn(Maybe.empty())
                .thenReturn(value.toMaybe())

        `when`(diskDAO.write(barCode, source))
                .thenReturn(Single.just(true))

        var result = store.get(barCode).blockingGet()
        assertThat(result.bar).isEqualTo(KEY)
        result = store.get(barCode).blockingGet()
        assertThat(result.bar).isEqualTo(KEY)
        verify(fetcher, times(1)).fetch(barCode)
    }

    private class Foo internal constructor() {
        internal var bar: String? = null
    }

    companion object {
        private const val KEY = "key"

        private fun source(data: String): BufferedSource = Okio.buffer(Okio.source(ByteArrayInputStream(data.toByteArray(UTF_8))))
    }
}
