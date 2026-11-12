package eu.kanade.domain

import eu.kanade.domain.chapter.interactor.GetAvailableScanlators
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.domain.download.interactor.DeleteDownload
import eu.kanade.domain.extension.interactor.GetExtensionLanguages
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.manga.interactor.GetExcludedScanlators
import eu.kanade.domain.manga.interactor.MassImport
import eu.kanade.domain.manga.interactor.SetExcludedScanlators
import eu.kanade.domain.manga.interactor.SetMangaViewerFlags
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.interactor.DeleteSourcePinGroup
import eu.kanade.domain.source.interactor.GetEnabledNovelSources
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.source.interactor.GetLanguagesWithSources
import eu.kanade.domain.source.interactor.GetNovelSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.GetSourcePinGroups
import eu.kanade.domain.source.interactor.GetSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.ManageFilterPresets
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.interactor.SetSourcePinGroups
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.SyncChapterProgressWithTrack
import eu.kanade.domain.track.interactor.TrackChapter
import mihon.data.extension.repository.ExtensionStoreRepositoryImpl
import mihon.data.extension.service.ExtensionStoreService
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import mihon.domain.extension.interactor.AddExtensionStore
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import mihon.domain.extension.interactor.GetExtensionStores
import mihon.domain.extension.interactor.RemoveExtensionStore
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.domain.migration.usecases.MigrateMangaUseCase
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.upcoming.interactor.GetUpcomingManga
import tachiyomi.data.anime.AnimeCategoryRepositoryImpl
import tachiyomi.data.anime.AnimeHistoryRepositoryImpl
import tachiyomi.data.anime.AnimeRepositoryImpl
import tachiyomi.data.anime.AnimeSourceRepositoryImpl
import tachiyomi.data.anime.AnimeTrackRepositoryImpl
import tachiyomi.data.anime.EpisodeRepositoryImpl
import tachiyomi.data.category.CategoryRepositoryImpl
import tachiyomi.data.chapter.ChapterRepositoryImpl
import tachiyomi.data.history.HistoryRepositoryImpl
import tachiyomi.data.manga.MangaRepositoryImpl
import tachiyomi.data.release.ReleaseServiceImpl
import tachiyomi.data.source.SourceRepositoryImpl
import tachiyomi.data.source.StubSourceRepositoryImpl
import tachiyomi.data.track.TrackRepositoryImpl
import tachiyomi.data.translation.TranslatedChapterRepositoryImpl
import tachiyomi.data.updates.UpdatesRepositoryImpl
import tachiyomi.domain.category.anime.interactor.AnimeCategories
import tachiyomi.domain.category.anime.interactor.SetAnimeCategories
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.chapter.interactor.GetBookmarkedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.interactor.GetChapterByUrlAndMangaId
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.RemoveChapters
import tachiyomi.domain.chapter.interactor.SearchChapterNames
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.chapter.interactor.ShouldUpdateDbChapter
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.InsertAnime
import tachiyomi.domain.entries.anime.interactor.UpdateAnime
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.history.anime.interactor.GetAnimeContinueWatching
import tachiyomi.domain.history.anime.interactor.UpsertAnimeHistory
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.interactor.GetTotalReadDuration
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.items.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.items.episode.interactor.InsertEpisodes
import tachiyomi.domain.items.episode.repository.EpisodeRepository
import tachiyomi.domain.library.anime.interactor.GetLibraryAnime
import tachiyomi.domain.library.interactor.RefreshLibraryCache
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.interactor.FindDuplicateNovels
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetFavoritesEntry
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMangaByUrlAndSourceId
import tachiyomi.domain.manga.interactor.GetMangaWithChapters
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.interactor.ResetViewerFlags
import tachiyomi.domain.manga.interactor.SearchMangaMetadata
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.interactor.SetMangaChapterFlags
import tachiyomi.domain.manga.interactor.UpdateMangaNotes
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.service.ReleaseService
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryManga
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.track.anime.interactor.AnimeTracks
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.GetTracksPerManga
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.repository.TrackRepository
import tachiyomi.domain.translation.repository.TranslatedChapterRepository
import tachiyomi.domain.updates.interactor.ClearUpdatesCache
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.repository.UpdatesRepository
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class DomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<CategoryRepository> { CategoryRepositoryImpl(get()) }
        addSingletonFactory<AnimeCategoryRepository> { AnimeCategoryRepositoryImpl(get()) }
        addFactory { AnimeCategories(get()) }
        addFactory { SetAnimeCategories(get()) }
        addFactory { GetCategories(get()) }
        addFactory { ResetCategoryFlags(get(), get()) }
        addFactory { SetDisplayMode(get()) }
        addFactory { SetSortModeForCategory(get(), get()) }
        addFactory { CreateCategoryWithName(get(), get()) }
        addFactory { RenameCategory(get()) }
        addFactory { ReorderCategory(get()) }
        addFactory { DeleteCategory(get(), get(), get()) }

        addSingletonFactory<MangaRepository> { MangaRepositoryImpl(get()) }
        addSingletonFactory<AnimeRepository> { AnimeRepositoryImpl(get()) }
        addFactory { GetAnime(get()) }
        addFactory { InsertAnime(get()) }
        addFactory { UpdateAnime(get()) }
        addFactory { GetLibraryAnime(get()) }
        addFactory { GetDuplicateLibraryManga(get(), get()) }
        addFactory { FindDuplicateNovels(get()) }
        addFactory { SearchMangaMetadata(get()) }
        addFactory { GetFavorites(get()) }
        addFactory { GetFavoritesEntry(get()) }
        // Singleton so all screens share the same cached SharedFlow for library queries
        addSingletonFactory { GetLibraryManga(get(), get()) }
        // Singleton for library cache management
        addSingletonFactory { RefreshLibraryCache(get()) }
        addFactory { GetMangaWithChapters(get(), get()) }
        addFactory { GetMangaByUrlAndSourceId(get()) }
        addFactory { GetManga(get()) }
        addFactory { SetCustomMangaInfo(get()) }
        addFactory { GetNextChapters(get(), get(), get()) }
        addFactory { GetUpcomingManga(get()) }
        addFactory { ResetViewerFlags(get()) }
        addFactory { SetMangaChapterFlags(get()) }
        addFactory { FetchInterval(get()) }
        addFactory { SetMangaDefaultChapterFlags(get(), get(), get()) }
        addFactory { SetMangaViewerFlags(get()) }
        addFactory { NetworkToLocalManga(get()) }
        addFactory { UpdateManga(get(), get()) }
        addFactory { UpdateMangaNotes(get()) }
        addFactory { SetMangaCategories(get(), get()) }
        addFactory { GetExcludedScanlators(get()) }
        addFactory { SetExcludedScanlators(get()) }
        addSingletonFactory { MassImport(get(), get(), get(), get()) }
        addFactory {
            MigrateMangaUseCase(
                get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            )
        }

        addSingletonFactory<ReleaseService> { ReleaseServiceImpl(get(), get()) }
        addFactory { GetApplicationRelease(get()) }

        addSingletonFactory<TrackRepository> { TrackRepositoryImpl(get()) }
        addSingletonFactory<AnimeTrackRepository> { AnimeTrackRepositoryImpl(get()) }
        addFactory { AnimeTracks(get()) }
        addFactory { TrackChapter(get(), get(), get(), get()) }
        addFactory { AddTracks(get(), get(), get(), get()) }
        addFactory { RefreshTracks(get(), get(), get(), get()) }
        addFactory { DeleteTrack(get()) }
        addFactory { GetTracksPerManga(get()) }
        addFactory { GetTracks(get()) }
        addFactory { InsertTrack(get()) }
        addFactory { SyncChapterProgressWithTrack(get(), get(), get()) }

        addSingletonFactory<ChapterRepository> { ChapterRepositoryImpl(get()) }
        addSingletonFactory<EpisodeRepository> { EpisodeRepositoryImpl(get()) }
        addFactory { GetEpisodesByAnimeId(get()) }
        addFactory { InsertEpisodes(get()) }
        addFactory { GetChapter(get()) }
        addFactory { GetChaptersByMangaId(get()) }
        addFactory { GetBookmarkedChaptersByMangaId(get()) }
        addFactory { GetChapterByUrlAndMangaId(get()) }
        addFactory { UpdateChapter(get()) }
        addFactory { SetReadStatus(get(), get(), get(), get(), get(), get()) }
        addFactory { ShouldUpdateDbChapter() }
        addFactory { SyncChaptersWithSource(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        addFactory { GetAvailableScanlators(get()) }
        addFactory { FilterChaptersForDownload(get(), get(), get()) }
        addFactory { RemoveChapters(get()) }
        addFactory { SearchChapterNames(get()) }

        addSingletonFactory<HistoryRepository> { HistoryRepositoryImpl(get()) }
        addSingletonFactory<AnimeHistoryRepository> { AnimeHistoryRepositoryImpl(get()) }
        addFactory { GetAnimeContinueWatching(get()) }
        addFactory { UpsertAnimeHistory(get()) }
        addFactory { GetHistory(get()) }
        addFactory { UpsertHistory(get()) }
        addFactory { RemoveHistory(get()) }
        addFactory { GetTotalReadDuration(get()) }

        addFactory { DeleteDownload(get(), get(), get()) }

        addFactory { GetExtensionsByType(get(), get()) }
        addFactory { GetExtensionSources(get()) }
        addFactory { GetExtensionLanguages(get(), get(), get()) }

        addSingletonFactory<UpdatesRepository> { UpdatesRepositoryImpl(get()) }
        addFactory { GetUpdates(get()) }
        addFactory { ClearUpdatesCache(get()) }

        // Translation
        addSingletonFactory<TranslatedChapterRepository> { TranslatedChapterRepositoryImpl(get(), get()) }

        addSingletonFactory<SourceRepository> { SourceRepositoryImpl(get(), get()) }
        addSingletonFactory<StubSourceRepository> { StubSourceRepositoryImpl(get()) }
        addSingletonFactory<AnimeSourceRepository> { AnimeSourceRepositoryImpl(get()) }
        addFactory { GetEnabledSources(get(), get(), get()) }
        addFactory { GetEnabledNovelSources(get(), get(), get()) }
        addFactory { GetLanguagesWithSources(get(), get()) }
        addFactory { GetRemoteManga(get()) }
        addFactory { GetSourcesWithFavoriteCount(get(), get()) }
        addFactory { GetNovelSourcesWithFavoriteCount(get(), get()) }
        addFactory { GetSourcesWithNonLibraryManga(get()) }
        addFactory { GetSourcePinGroups(get()) }
        addFactory { SetMigrateSorting(get()) }
        addFactory { SetSourcePinGroups(get()) }
        addFactory { ToggleLanguage(get()) }
        addFactory { ToggleSource(get()) }
        addFactory { ToggleSourcePin(get()) }
        addFactory { DeleteSourcePinGroup(get()) }
        addSingletonFactory { TrustExtension(get(), get()) } // Singleton to enable caching of trusted fingerprints

        addSingletonFactory { ExtensionStoreService(get(), get()) }
        addSingletonFactory<ExtensionStoreRepository> { ExtensionStoreRepositoryImpl(get(), get()) }
        addFactory { AddExtensionStore(get()) }
        addFactory { GetExtensionStoreCountAsFlow(get()) }
        addFactory { GetExtensionStores(get()) }
        addFactory { RemoveExtensionStore(get()) }
        addFactory { UpdateExtensionStores(get()) }

        addFactory { ToggleIncognito(get()) }
        addFactory { GetIncognitoState(get(), get(), get()) }
        addFactory { ManageFilterPresets(get()) }

        addFactory { UpdateMangaFromRemote(get(), get(), get(), get(), get(), get(), get(), get()) }
    }
}
