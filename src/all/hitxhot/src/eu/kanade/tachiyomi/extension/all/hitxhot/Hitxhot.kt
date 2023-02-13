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
import org.jsoup.select.Evaluator
import java.text.SimpleDateFormat
import java.util.Locale

class Hitxhot : ParsedHttpSource() {

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
        val mangasClass = document.selectFirst(Evaluator.Class("videos")).children()
        val mangas = mutableListOf<SManga>()
        for (index in 0 until mangasClass.size) {
            if (!mangasClass[index].`is`(Evaluator.Tag("style"))) {
                val cardBody = mangasClass[index].selectFirst(Evaluator.Class("thcovering-video"))
                val info = cardBody.selectFirst(Evaluator.Class("denomination"))
                if (info != null) {
                    val image = cardBody.selectFirst(Evaluator.Class("xld")).attr("src")
                    val mUrl = info.attr("href")
                    val mTitle = info.text()
                    val m = SManga.create().apply {
                        url = mUrl
                        title = mTitle
                        thumbnail_url = image
                        status = SManga.COMPLETED
                        initialized = false
                    }
                    mangas.add(m)
                }
            }
        }

        val lastTag = document.select(".pagination-site > li > a").last()
        return MangasPage(mangas, lastTag.hasClass("next"))
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
            query.isNotEmpty() -> GET("$baseUrl/?search=$query&start=${20 * (page - 1)}")
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
        val lastTag = document.select(".c-content > div")
        val isSingle = !lastTag.hasClass("pag")
        if (isSingle) {
            val chapter = SChapter.create()
            chapter.setUrlWithoutDomain(response.request.url.toString())
            chapter.chapter_number = 1f
            chapter.name = "Page: 1"
            chapter.date_upload = SimpleDateFormat(
                "EEE MMM MM yyyy",
                Locale.US
            ).parse(document.select(".it-date").text())?.time ?: 0L
            chapterPage.add(chapter)
            return chapterPage
        }

        val s = document.select(".pagination-site > li > a")
        s.forEachIndexed { index, element ->
            if (element.text().toIntOrNull() != null) {
                val chapter = SChapter.create()
                chapter.setUrlWithoutDomain(element.attr("abs:href"))
                chapter.chapter_number = index.toFloat()
                chapter.name = "Page: ${index + 1}"
                chapter.date_upload = SimpleDateFormat(
                    "EEE MMM MM yyyy",
                    Locale.US
                ).parse(document.select(".it-date").text())?.time ?: 0L
                chapterPage.add(chapter)
            }
        }

        val lastChapter = chapterPage.last()
        val hasNext = s.last().text().contains("next", ignoreCase = true)
        val lastUrl = if (lastChapter.url.endsWith("#")) {
            lastChapter.url.replace("#", "&page=1")
        } else {
            lastChapter.url
        }
        visNextChapter(
            baseUrl + lastUrl,
            s.size - 1,
            lastChapter.chapter_number,
            hasNext,
            chapterPage
        )
        chapterPage.reverse()
        return chapterPage
    }

    private fun visNextChapter(
        lastUrl: String,
        lastPage: Int,
        lastChapterNum: Float,
        hasNext: Boolean,
        chapterPage: MutableList<SChapter>
    ) {
        if (hasNext) {
            val newUrl = lastUrl.replace("page=$lastPage", "page=${lastPage + 1}")
            val c = client.newCall(GET(newUrl)).execute().asJsoup()
            val chapter = SChapter.create()
            chapter.setUrlWithoutDomain(newUrl)
            chapter.chapter_number = lastChapterNum + 1
            chapter.name = "Page: ${lastPage + 1}"
            chapter.date_upload = SimpleDateFormat(
                "EEE MMM MM yyyy",
                Locale.US
            ).parse(c.select(".it-date").text())?.time ?: 0L
            chapterPage.add(chapter)

            val lastChapter = chapterPage.last()
            val s = c.select(".pagination-site > li > a")
            val mHasNext = s.last().text().contains("next", ignoreCase = true)
            visNextChapter(
                baseUrl + lastChapter.url,
                chapterPage.size - 1,
                lastChapterNum + 1,
                mHasNext,
                chapterPage
            )
        }
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
