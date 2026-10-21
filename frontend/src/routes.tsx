import type { RouteObject } from 'react-router-dom';
import { RootLayout } from './components/layout/RootLayout';
import { HomePage } from './pages/HomePage';
import { CatalogPage } from './pages/CatalogPage';
import { MoviePage } from './pages/MoviePage';
import { RecommendPage } from './pages/RecommendPage';
import { ChatPage } from './pages/ChatPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { AdminLayout } from './pages/admin/AdminLayout';
import { AdminMoviesPage } from './pages/admin/AdminMoviesPage';
import { AdminMovieFormPage } from './pages/admin/AdminMovieFormPage';
import { AdminDictionaryPage } from './pages/admin/AdminDictionaryPage';

/** Все маршруты приложения. Экспортируются отдельно, чтобы тесты могли собрать memory-router. */
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'catalog', element: <CatalogPage /> },
      { path: 'movies/:id', element: <MoviePage /> },
      { path: 'recommend', element: <RecommendPage /> },
      { path: 'chat', element: <ChatPage /> },
      {
        path: 'admin',
        element: <AdminLayout />,
        children: [
          { index: true, element: <AdminMoviesPage /> },
          { path: 'movies/new', element: <AdminMovieFormPage /> },
          { path: 'movies/:id/edit', element: <AdminMovieFormPage /> },
          { path: 'genres', element: <AdminDictionaryPage key="genres" kind="genres" /> },
          { path: 'moods', element: <AdminDictionaryPage key="moods" kind="moods" /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
