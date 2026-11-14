import './app.css';
import { mount } from 'svelte';
import App from './App.svelte';
import { router } from './lib/router.svelte';
import { union } from './lib/store.svelte';

router.init();

// Android's back gesture: close a sheet first, otherwise let the shell use
// WebView history. Returning false tells the shell to go back or background us.
window.unionAndroidBack = () => {
  if (document.querySelector('[role="dialog"]')) {
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    return true;
  }
  return false;
};

const target = document.getElementById('app');
if (!target) throw new Error('Union could not find its mount point.');

const app = mount(App, { target });

void union.init().catch((error) => {
  console.error('Union failed to initialise', error);
});

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    const base = location.href.replace(/[^/]*$/, '');
    navigator.serviceWorker.register(new URL('sw.js', base).href, { scope: './' }).catch((error) => {
      console.warn('Service worker registration failed', error);
    });
  });
}

if (navigator.storage?.persist) {
  void navigator.storage.persist().catch(() => undefined);
}

export default app;
