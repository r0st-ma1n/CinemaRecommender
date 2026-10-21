import type {
  AuthStatus,
  ChatReply,
  ChatRequest,
  Criteria,
  DictionaryItem,
  DictionaryKind,
  Movie,
  MovieInput,
  MoodSuggestion,
  Recommendation,
} from '../types';

/** Всё, что фронту нужно от бэкенда. Две реализации: realApi (HTTP) и mockApi (в памяти). */
export interface Api {
  getMovies(): Promise<Movie[]>;
  getMovie(id: number): Promise<Movie | null>;
  getTypes(): Promise<DictionaryItem[]>;
  getGenres(): Promise<DictionaryItem[]>;
  getMoods(): Promise<DictionaryItem[]>;
  recommend(criteria: Criteria): Promise<Recommendation[]>;
  sendChat(request: ChatRequest): Promise<ChatReply>;

  getAuth(): Promise<AuthStatus>;
  login(username: string, password: string): Promise<AuthStatus>;
  logout(): Promise<void>;

  createMovie(input: MovieInput): Promise<Movie>;
  updateMovie(id: number, input: MovieInput): Promise<Movie>;
  deleteMovie(id: number): Promise<void>;
  suggestMoods(description: string, genreIds: number[]): Promise<MoodSuggestion[]>;

  createDictionaryItem(kind: DictionaryKind, name: string): Promise<DictionaryItem>;
  renameDictionaryItem(kind: DictionaryKind, id: number, name: string): Promise<DictionaryItem>;
  deleteDictionaryItem(kind: DictionaryKind, id: number): Promise<void>;
}
