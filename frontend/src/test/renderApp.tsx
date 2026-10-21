import { render } from '@testing-library/react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';
import { routes } from '../routes';

/** Всё приложение с роутером в памяти, начиная с заданного адреса. */
export function renderApp(url = '/') {
  const router = createMemoryRouter(routes, { initialEntries: [url] });
  const view = render(<RouterProvider router={router} />);
  return { router, ...view };
}
