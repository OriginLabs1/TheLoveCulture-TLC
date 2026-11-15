<script lang="ts">
  import { onMount } from 'svelte';
  import BottomNav from './components/BottomNav.svelte';
  import Logo from './components/Logo.svelte';
  import Toasts from './components/Toasts.svelte';
  import { router } from './lib/router.svelte';
  import { union } from './lib/store.svelte';
  import Onboarding from './screens/Onboarding.svelte';
  import Home from './screens/Home.svelte';
  import Explore from './screens/Explore.svelte';
  import People from './screens/People.svelte';
  import Messages from './screens/Messages.svelte';
  import Communities from './screens/Communities.svelte';
  import Learning from './screens/Learning.svelte';
  import Experiences from './screens/Experiences.svelte';
  import Presence from './screens/Presence.svelte';
  import Ai from './screens/Ai.svelte';
  import Economy from './screens/Economy.svelte';
  import Mentorship from './screens/Mentorship.svelte';
  import Operations from './screens/Operations.svelte';
  import Settings from './screens/Settings.svelte';
  import More from './screens/More.svelte';
  import LinkResolver from './screens/LinkResolver.svelte';

  let boot = $state(true);

  /* Keep the first-run flow mounted after the identity exists, so the
     encrypted-backup step is actually reachable. It closes only from
     Onboarding's own finish(), or when no identity exists at all. */
  let showOnboarding = $state(false);
  let onboardingClosed = $state(false);

  $effect(() => {
    if (!union.ready) return;
    if (!union.identity) {
      showOnboarding = true;
      onboardingClosed = false;
    }
  });

  function closeOnboarding() {
    onboardingClosed = true;
    showOnboarding = false;
  }

  onMount(() => {
    const started = Date.now();
    const tick = () => {
      if (union.ready || Date.now() - started > 8000) boot = false;
      else setTimeout(tick, 60);
    };
    setTimeout(tick, 40);
  });

  const segments = $derived(router.segments);
  const head = $derived(segments[0] ?? '');
  const isLink = $derived(head === 'u' || head === 'l');

  function renderRoute() {
    switch (head) {
      case '':
        return Home;
      case 'explore':
        return Explore;
      case 'people':
        return People;
      case 'messages':
        return Messages;
      case 'communities':
        return Communities;
      case 'learning':
        return Learning;
      case 'experiences':
        return Experiences;
      case 'presence':
        return Presence;
      case 'ai':
        return Ai;
      case 'economy':
        return Economy;
      case 'mentorship':
        return Mentorship;
      case 'operations':
        return Operations;
      case 'settings':
        return Settings;
      case 'more':
        return More;
      case 'u':
      case 'l':
        return LinkResolver;
      default:
        return Home;
    }
  }

  const screen = $derived(renderRoute());
</script>

{#if boot || !union.ready}
  <div class="boot">
    <div class="bootorb" aria-hidden="true">
      <span class="bring b1"></span>
      <span class="bring b2"></span>
      <span class="bmark"><Logo size={52} glow /></span>
    </div>
    <div class="boottype">
      <h1 class="display">Union</h1>
      <p class="muted small">One connected environment</p>
    </div>
    <div class="loader" aria-label="Opening"><span></span></div>
    <p class="bootfine">Local first · keys on this device</p>
  </div>
{:else if union.startupError}
  <div class="fatal">
    <Logo size={46} />
    <h1 class="display">Union could not open</h1>
    <p class="muted">{union.startupError}</p>
    <button class="btn primary" onclick={() => location.reload()}>Try again</button>
    <p class="tiny faint">Your records remain on this device. Nothing was uploaded.</p>
  </div>
{:else if !isLink && (showOnboarding || (!union.identity && !onboardingClosed))}
  <Onboarding onDone={closeOnboarding} />
{:else}
  {@const Screen = screen}
  <Screen />
  {#if union.identity && !isLink}<BottomNav />{/if}
{/if}

<Toasts />

<style>
  .boot {
    min-height: 100vh;
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    text-align: center;
    padding: 32px;
    animation: fade-in 500ms var(--ease) both;
  }
  .bootorb {
    position: relative;
    width: 118px;
    height: 118px;
    display: grid;
    place-items: center;
    margin-bottom: 2px;
  }
  .bring {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    border: 1px solid var(--line);
  }
  .b1 { inset: 8px; border-style: dashed; animation: spin 26s linear infinite; }
  .b2 { border-color: color-mix(in srgb, var(--accent) 22%, transparent); animation: spin 34s linear infinite reverse; }
  .bmark {
    width: 72px; height: 72px;
    border-radius: 26px;
    display: grid; place-items: center;
    background:
      radial-gradient(circle at 30% 24%, rgb(255 255 255 / 0.9), transparent 62%),
      var(--surface);
    border: 1px solid var(--line);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.8), 0 26px 60px -24px rgb(23 24 29 / 0.34);
  }
  .bootorb::before {
    content: "";
    position: absolute;
    inset: 16px;
    border-radius: 50%;
    background: radial-gradient(circle, color-mix(in srgb, var(--accent) 22%, transparent), transparent 68%);
    filter: blur(20px);
    animation: breathe 3.6s ease-in-out infinite alternate;
  }
  .boottype { display: flex; flex-direction: column; gap: 4px; }
  .boot h1 { margin: 0; font-size: 2.5rem; letter-spacing: -0.045em; }
  .boot p { margin: 0; letter-spacing: 0.035em; }
  .loader {
    width: 104px;
    height: 4px;
    border-radius: 999px;
    background: var(--surface-3);
    overflow: hidden;
    margin-top: 10px;
    position: relative;
  }
  .loader span {
    position: absolute;
    inset: 0;
    width: 40%;
    border-radius: inherit;
    background: linear-gradient(90deg, transparent, var(--accent), var(--teal), transparent);
    animation: slide 1.35s var(--ease) infinite;
  }
  .bootfine { font-size: 0.64rem; color: var(--faint); letter-spacing: 0.12em; text-transform: uppercase; }
  .fatal {
    min-height: 100vh;
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    text-align: center;
    padding: 28px;
    max-width: 480px;
    margin: 0 auto;
  }
  .fatal h1 { margin: 8px 0 0; font-size: 1.6rem; }
  .fatal p { margin: 0; max-width: 40ch; }
  @keyframes spin { to { transform: rotate(360deg); } }
  @keyframes breathe { to { opacity: 0.4; transform: scale(1.08); } }
  @keyframes slide { from { transform: translateX(-110%); } to { transform: translateX(260%); } }
  @keyframes fade-in { from { opacity: 0; transform: scale(0.98); } }
</style>
