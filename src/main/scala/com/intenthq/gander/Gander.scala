package com.intenthq.gander

import java.util.Date

import com.intenthq.gander.extractors.ContentExtractor._
import com.intenthq.gander.opengraph.OpenGraphData
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.convert.Wrappers.JListWrapper
import scala.util.Try


case class Link(text: String, target: String)

case class PageInfo(title: String,
                    processedTitle: String,
                    metaDescription: String,
                    metaKeywords: String,
                    lang: Option[String],
                    canonicalLink: Option[String],
                    openGraphData: OpenGraphData,
                    cleanedText: Option[String] = None,
                    links: Seq[Link] = Seq.empty,
                    publishDate: Option[Date] = None)

object Gander {

  def extract(html: String, lang: String = "all"): Option[PageInfo] =
    Try(Jsoup.parse(html)).toOption.map { doc =>
      val canonicalLink = extractCanonicalLink(doc)
      val publishDate = extractDate(doc).map(_.toDate).orElse(canonicalLink.flatMap(extractDateFromURL).map(_.toDate))

      val rawTitle = extractTitle(doc)
      val info = PageInfo(title = rawTitle,
                          processedTitle = processTitle(rawTitle, canonicalLink),
                          metaDescription = extractMetaDescription(doc),
                          metaKeywords = extractMetaKeywords(doc),
                          lang = extractLang(doc),
                          canonicalLink = canonicalLink,
                          publishDate = publishDate,
                          openGraphData = OpenGraphData(doc)
      )

      val cleanedDoc = DocumentCleaner.clean(doc)
      calculateBestNodeBasedOnClustering(cleanedDoc, lang).map { node =>
        //some mutability beauty
        postExtractionCleanup(node, lang)
        info.copy(cleanedText = Some(node.text()),
                  links = extractLinks(node))
      }.getOrElse(info)
    }
}
