package com.metrolist.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class SectionListRenderer(
    val header: Header?,
    val contents: List<Content>?,
    val continuations: List<Continuation>?,
) {
    @Serializable
    data class Header(
        val chipCloudRenderer: ChipCloudRenderer?,
    ) {
        @Serializable
        data class ChipCloudRenderer(
            val chips: List<Chip>,
        ) {
            @Serializable
            data class Chip(
                val chipCloudChipRenderer: ChipCloudChipRenderer,
            ) {
                @Serializable
                data class ChipCloudChipRenderer(
                    val isSelected: Boolean = false,
                    val navigationEndpoint: NavigationEndpoint? = null,
                    val onDeselectedCommand: NavigationEndpoint? = null,
                    // The close button doesn't have the following two fields
                    val text: Runs?,
                    val uniqueId: String?,
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Content(
        @JsonNames("musicImmersiveCarouselShelfRenderer")
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer?,
        val musicShelfRenderer: MusicShelfRenderer?,
        val musicCardShelfRenderer: MusicCardShelfRenderer?,
        val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer?,
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer?,
        val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer?,
        val gridRenderer: GridRenderer?,
        val itemSectionRenderer: ItemSectionRenderer?,
    )
}

@Serializable
data class ItemSectionRenderer(
    val contents: List<Content>?,
    val header: Header? = null,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val musicShelfRenderer: MusicShelfRenderer? = null,
        val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer? = null,
        val gridRenderer: GridRenderer? = null,
        // Spelling notes at the top of search results: "Did you mean: X" (results are still for
        // what was typed) and "Showing results for: X" (YouTube already searched X instead).
        val didYouMeanRenderer: SpellCorrectionRenderer? = null,
        val showingResultsForRenderer: SpellCorrectionRenderer? = null,
    )

    @Serializable
    data class SpellCorrectionRenderer(
        val correctedQuery: Runs? = null,
        val correctedQueryEndpoint: NavigationEndpoint? = null,
    )

    @Serializable
    data class Header(
        val itemSectionTabbedHeaderRenderer: ItemSectionTabbedHeaderRenderer?,
    )

    @Serializable
    data class ItemSectionTabbedHeaderRenderer(
        val title: Runs?,
    )
}
