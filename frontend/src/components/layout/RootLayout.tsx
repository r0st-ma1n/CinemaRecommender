import { Outlet, ScrollRestoration } from 'react-router-dom';
import { Header } from './Header';
import { Footer } from './Footer';
import { ToastProvider } from '../Toast';
import { apiMode } from '../../api';

export function RootLayout() {
  return (
    <ToastProvider>
      <div className="flex min-h-screen flex-col">
        <Header />
        {apiMode === 'mock' && (
          <div className="border-b border-accent/20 bg-accent/5 px-4 py-1.5 text-center text-xs text-accent/90">
            Демо-режим: данные из встроенных моков, правки сбрасываются при перезагрузке
          </div>
        )}
        <main className="mx-auto w-full max-w-7xl flex-1 px-4 pt-8 sm:px-6 sm:pt-12">
          <Outlet />
        </main>
        <Footer />
      </div>
      <ScrollRestoration />
    </ToastProvider>
  );
}
