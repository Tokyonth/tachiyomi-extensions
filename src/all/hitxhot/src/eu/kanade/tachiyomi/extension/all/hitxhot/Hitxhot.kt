package eu.kanade.tachiyomi.extension.all.hitxhot

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class Hitxhot : ParsedHttpSource() {

    private val dateFormat by lazy {
        SimpleDateFormat("EEE MMM MM yyyy", Locale.US)
    }

    override val baseUrl = "https://hitxhot.com"

    override val lang = "all"

    override val name = "Hitxhot"

    override val supportsLatest = true

    // Latest
    override fun latestUpdatesFromElement(element: Element) =
        throw UnsupportedOperationException("Not used")

    override fun latestUpdatesNextPageSelector() =
        throw UnsupportedOperationException("Not used")

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/?m=1&page=$page")
    }

    override fun latestUpdatesSelector() =
        throw UnsupportedOperationException("Not used")

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = mutableListOf<SManga>()
        val mangasClass = document.select(".thcovering-video > ins > a")
        mangasClass.forEach {
            val mUrl = it.attr("href")
            val mTitle = it.attr("title")
            val thumbnail = it.selectFirst(".xld").attr("src")
            val mange = SManga.create().apply {
                url = mUrl
                title = mTitle
                thumbnail_url = thumbnail
                status = SManga.COMPLETED
                initialized = false
            }
            mangas.add(mange)
        }

        val lastPageTag = document.select(".pagination-site > li > a").last()
        return MangasPage(mangas, lastPageTag.hasClass("next"))
    }

    // Popular
    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)
    override fun popularMangaNextPageSelector() = throw UnsupportedOperationException("Not used")
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/hot?m=1&page=$page")
    }

    override fun popularMangaSelector() =
        throw UnsupportedOperationException("Not used")

    override fun popularMangaParse(response: Response) = latestUpdatesParse(response)

    // Search
    override fun searchMangaFromElement(element: Element) = latestUpdatesFromElement(element)
    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return when {
            query.isNotEmpty() -> GET("$baseUrl/?search=$query")
            else -> popularMangaRequest(page)
        }
    }

    override fun searchMangaParse(response: Response) = latestUpdatesParse(response)

    override fun searchMangaSelector() = latestUpdatesSelector()

    // Details
    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        val genres = mutableListOf<String>()
        document.select(".it-cat-content")[1].select("a").forEach {
            genres.add(it.text())
        }
        manga.genre = genres.joinToString(", ")
        return manga
    }

    override fun chapterFromElement(element: Element) =
        throw UnsupportedOperationException("Not used")

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapterPage = mutableListOf<SChapter>()
        val title = document.selectFirst(".box-mt-output").text()
        val sIndex = title.indexOf("page", ignoreCase = true)
        val pageNum = if (sIndex < 0) {
            1
        } else {
            val page = title.substring(sIndex + 5, title.length)
            page.split("/")[1].toInt()
        }

        val time = document.select(".it-date").text()
        for (pIndex in pageNum downTo 1) {
            val url = response.request.url.toString().removeSuffix("?m=1")
            val chapter = SChapter.create()
            chapter.setUrlWithoutDomain(url.plus("?page=$pIndex"))
            chapter.chapter_number = pIndex.toFloat()
            chapter.name = "Page: $pIndex"
            chapter.date_upload = dateFormat.parse(time)?.time ?: 0L
            chapterPage.add(chapter)
        }
        return chapterPage
    }

    override fun chapterListSelector() = throw UnsupportedOperationException("Not used")

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        document.selectFirst(".contentme").select("img").forEach {
            val itUrl = it.attr("abs:src")
            pages.add(Page(pages.size, "", itUrl))
        }
        return pages
    }

    override fun imageUrlParse(document: Document): String =
        throw UnsupportedOperationException("Not used")
}
