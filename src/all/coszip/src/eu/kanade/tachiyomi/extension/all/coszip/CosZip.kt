package eu.kanade.tachiyomi.extension.all.coszip

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.*

class CosZip : HttpSource() {

    override val name = "CosZip"

    override val lang = "all"

    override val supportsLatest = false

    override val baseUrl = "https://www.coszip.com"

    private val dateFormat by lazy {
        SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
    }

    override fun popularMangaRequest(page: Int) = GET(baseUrl, headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = mutableListOf<SManga>()
        val mangasDoc = document.select(".jeg_posts.jeg_load_more_flag > article")
        mangasDoc.forEach {
            val mTitle = it.selectFirst(".jeg_post_title > a")
            val mThumbnail = it.selectFirst(".thumbnail-container > img")
            val mUrl = mTitle!!.attr("href").replace(baseUrl, "")
            val m = SManga.create().apply {
                url = mUrl
                title = mTitle.text()
                //  thumbnail_url = mThumbnail!!.attr("data-src")
                status = SManga.COMPLETED
                initialized = true
            }
            mangas.add(m)
        }
        return MangasPage(mangas, false)
    }

    override fun latestUpdatesRequest(page: Int) =
        throw UnsupportedOperationException("Not used.")

    override fun latestUpdatesParse(response: Response) =
        throw UnsupportedOperationException("Not used.")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotEmpty()) {
            val url = "$baseUrl/?s=$query"
            return GET(url, headers)
        }

        val filter =
            filters.filterIsInstance<RegionFilter>().firstOrNull() ?: return popularMangaRequest(
                page,
            )
        return GET("$baseUrl/${filter.name}", headers)
    }

    override fun searchMangaParse(response: Response) = popularMangaParse(response)

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val tagsDoc = document.select(".jeg_post_tags > a")
        val tags = tagsDoc.joinToString {
            it.text()
        }
        return SManga.create().apply {
            genre = tags
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val pageInfo = document.selectFirst(".page_info")!!.text()
        val pageIndex = pageInfo.replace("Page ", "", ignoreCase = true)
        val pageCount = pageIndex.split(" of ")[1].toInt()
        val time = document.selectFirst(".jeg_meta_date > a")!!.text()

        val oUrl = response.request.url.toString().removeSuffix("?amp=1").replace(baseUrl, "")
        val chapters = mutableListOf<SChapter>()
        for (index in pageCount downTo 1) {
            val mUrl = oUrl.plus("/$index")
            val chapter = SChapter.create().apply {
                url = mUrl
                name = "Page: $index"
                date_upload = dateFormat.parse(time)!!.time
                chapter_number = index.toFloat()
            }
            chapters.add(chapter)
        }
        return chapters
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val pagesDoc = document.select(".content-inner > p > div")
        val pages = mutableListOf<Page>()
        pagesDoc.forEach {
            val url = it.selectFirst("amp-img")!!.attr("src")
            val page = Page(pages.size, imageUrl = url)
            pages.add(page)
        }
        return pages
    }

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used.")

    override fun getFilterList() = FilterList(RegionFilter())

    private class RegionFilter : Filter.Select<String>(
        "Category",
        arrayOf("All", "COSPLAY", "Erotica/ Nude", "Photobook"),
    )
}
