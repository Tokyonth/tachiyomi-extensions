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
            val mThumbnail = it.selectFirst(".thumbnail-container > img")!!.attr("data-src")
            // val thumbUrl = mThumbnail.split(", ")[0]
            val mUrl = mTitle!!.attr("href")

            val m = SManga.create().apply {
                title = mTitle.text()
                setUrlWithoutDomain(mUrl)
                thumbnail_url = mThumbnail.plus("&ssl=1")
                status = SManga.COMPLETED
                initialized = true
            }
            mangas.add(m)
        }
        val hasMore = document.hasClass("jeg_block_loadmore")
        return MangasPage(mangas, hasMore)
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
            filters.filterIsInstance<CategoryFilter>().firstOrNull() ?: return popularMangaRequest(
                page,
            )
        val s = filter.values[filter.state].second
        return GET("$baseUrl/$s", headers)
    }

    override fun searchMangaParse(response: Response) = popularMangaParse(response)

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val tagsDoc = document.select(".jeg_post_tags > a")
        val tags = tagsDoc.joinToString {
            it.text()
        }
        val mAuthor = document.select(".content-inner > p")[1].text()
        val desc = document.select(".content-inner > p")[2].text()
        return SManga.create().apply {
            author = mAuthor.replace("Model name: ", "")
            description = desc
            genre = tags
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val pageInfo = document.selectFirst(".page_info")!!.text()
        val pageIndex = pageInfo.replace("Page ", "", ignoreCase = true)
        val pageCount = pageIndex.split(" of ")[1].toInt()
        val time = document.selectFirst(".jeg_meta_date > a")!!.text()

        val oUrl = response.request.url.toString().removeSuffix("?amp=1")
        val chapters = mutableListOf<SChapter>()
        for (index in pageCount downTo 1) {
            val mUrl = oUrl.plus("/$index")
            val chapter = SChapter.create().apply {
                setUrlWithoutDomain(mUrl)
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
        val pagesDoc = document.select(".content-inner > p > img")
        val pages = mutableListOf<Page>()
        pagesDoc.forEach {
            val url = it!!.attr("src")
            val page = Page(pages.size, imageUrl = url)
            pages.add(page)
        }
        return pages
    }

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used.")

    override fun getFilterList() = FilterList(CategoryFilter())

    private class CategoryFilter : Filter.Select<Pair<String, String>>(
        "Category",
        arrayOf(
            Pair("All", ""),
            Pair("COSPLAY", ""),
            Pair("Erotica/ Nude", ""),
            Pair("Photobook", ""),
        ),
    )
}
