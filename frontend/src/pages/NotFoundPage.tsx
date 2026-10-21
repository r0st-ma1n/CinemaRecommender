import { ButtonLink } from '../components/ui';

export function NotFoundPage({ what = 'Страница не найдена' }: { what?: string }) {
  return (
    <div className="flex flex-col items-center gap-5 py-24 text-center">
      <p className="text-7xl font-extrabold text-accent">404</p>
      <h1 className="text-2xl font-bold">{what}</h1>
      <p className="text-muted">Возможно, её удалили или адрес набран с ошибкой.</p>
      <ButtonLink to="/">На главную</ButtonLink>
    </div>
  );
}
