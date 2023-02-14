package eu.kanade.tachiyomi.extension.all.coszip

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class CosZip : HttpSource() {

    override val baseUrl = "https://www.coszip.com"

    override val lang = "all"

    override val name = "CosZip"

    override val supportsLatest = false

    private val json: Json by injectLazy()

    override fun chapterListParse(response: Response): List<SChapter> {
        TODO("Not yet implemented")
    }

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun latestUpdatesParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun latestUpdatesRequest(page: Int) =
        throw UnsupportedOperationException("Not used")

    override fun mangaDetailsParse(response: Response): SManga {
        TODO("Not yet implemented")
    }

    override fun pageListParse(response: Response): List<Page> {
        TODO("Not yet implemented")
    }

    override fun popularMangaParse(response: Response): MangasPage {
        TODO("Not yet implemented")
    }

    override fun popularMangaRequest(page: Int): Request {
        return GET(baseUrl)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        TODO("Not yet implemented")
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        TODO("Not yet implemented")
    }

}
