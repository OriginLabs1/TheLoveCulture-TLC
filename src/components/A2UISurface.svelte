<script lang="ts">
  import Icon from './Icon.svelte';
  import { router } from '../lib/router.svelte';
  import { copy } from '../lib/util';
  import { toasts } from '../lib/toast.svelte';
  import { haptic } from '../lib/native';
  import type { A2UIAction, A2UISurface } from '../lib/a2ui';

  let {
    surface,
    onaction,
    label = 'Composed surface'
  }: {
    surface: A2UISurface;
    onaction?: (action: A2UIAction, values: Record<string, string>) => void;
    label?: string;
  } = $props();

  let values = $state<Record<string, string>>({});

  $effect(() => {
    for (const node of surface.nodes) {
      if (node.type !== 'form') continue;
      for (const field of node.fields) {
        if (values[field.name] === undefined) values[field.name] = field.value ?? '';
      }
    }
  });

  function fill(template: string): string {
    return template.replace(/\{([a-zA-Z][a-zA-Z0-9_]*)\}/g, (_, key: string) => values[key] ?? '');
  }

  async function act(action: A2UIAction) {
    haptic('tap');
    if (action.type === 'navigate') {
      router.go(action.to);
      return;
    }
    if (action.type === 'copy') {
      const ok = await copy(action.text);
      toasts.push(ok ? 'Copied.' : 'Could not copy.', ok ? 'success' : 'warn');
      return;
    }
    if (action.type === 'ask') {
      onaction?.({ type: 'ask', text: fill(action.text) }, { ...values });
      return;
    }
    onaction?.(action, { ...values });
  }
</script>

<section class="a2ui" aria-label={label}>
  <div class="surface-tag">
    <span class="tagmark"><Icon name="spark" size={11} /></span>
    <span>{label}</span>
    <span class="tagver">a2ui/1</span>
  </div>

  {#each surface.nodes as node, index (index)}
    {#if node.type === 'heading'}
      <header class="surface-head">
        <h3>{node.title}</h3>
        {#if node.subtitle}<p>{node.subtitle}</p>{/if}
      </header>
    {:else if node.type === 'text'}
      <p class="a2ui-text" class:display={node.style === 'display'} class:caption={node.style === 'caption'} class:label={node.style === 'label'}>{node.text}</p>
    {:else if node.type === 'stats'}
      <div class="a2ui-stats">
        {#each node.items as stat (stat.label)}
          <div class="a2ui-stat">
            <strong>{stat.value}</strong>
            <span>{stat.label}</span>
            {#if stat.hint}<em>{stat.hint}</em>{/if}
          </div>
        {/each}
      </div>
    {:else if node.type === 'progress'}
      <div class="surface-progress">
        <div class="progress-label">
          <span>{node.label ?? 'Progress'}</span><strong>{node.value}%</strong>
        </div>
        <div class="progress" style={`--p:${node.value}`}><span></span></div>
      </div>
    {:else if node.type === 'list'}
      {#if node.title}<p class="surface-list-title">{node.title}</p>{/if}
      <ul class="a2ui-list">
        {#each node.items as item (item.title)}
          <li>
            {#if item.action}
              <button class="rowitem" onclick={() => item.action && act(item.action)}>
                <span class="rowtext"><strong>{item.title}</strong>{#if item.text}<span>{item.text}</span>{/if}</span>
                <span class="rowtrail">{item.value}<Icon name="chevron-right" size={14} /></span>
              </button>
            {:else}
              <div class="rowitem static">
                <span class="rowtext"><strong>{item.title}</strong>{#if item.text}<span>{item.text}</span>{/if}</span>
                {#if item.value}<span class="rowtrail">{item.value}</span>{/if}
              </div>
            {/if}
          </li>
        {/each}
      </ul>
    {:else if node.type === 'form'}
      <form class="a2ui-form" onsubmit={(event) => { event.preventDefault(); act(node.submit.action); }}>
        {#if node.title}<p class="surface-list-title">{node.title}</p>{/if}
        {#each node.fields as field (field.name)}
          <div class="field">
            <label for={`a2ui-${surface.id}-${field.name}`}>{field.label}</label>
            {#if field.type === 'textarea'}
              <textarea id={`a2ui-${surface.id}-${field.name}`} bind:value={values[field.name]} placeholder={field.placeholder ?? ''} rows="3"></textarea>
            {:else}
              <input id={`a2ui-${surface.id}-${field.name}`} type={field.type === 'number' ? 'number' : 'text'} bind:value={values[field.name]} placeholder={field.placeholder ?? ''} />
            {/if}
          </div>
        {/each}
        <div class="a2ui-actions">
          <button type="submit" class="btn primary" disabled={node.fields.some((f) => !values[f.name]?.trim())}>
            {node.submit.label}
          </button>
        </div>
      </form>
    {:else if node.type === 'actions'}
      <div class="a2ui-actions">
        {#each node.items as button (button.label)}
          <button class="btn" class:primary={button.variant === 'primary'} class:soft={button.variant === 'soft'} class:ghost={button.variant === 'ghost'} onclick={() => act(button.action)}>
            {#if button.icon}<Icon name={button.icon} size={14} />{/if}{button.label}
          </button>
        {/each}
      </div>
    {:else if node.type === 'note'}
      <p class="a2ui-note">{node.text}</p>
    {:else if node.type === 'divider'}
      <hr class="a2ui-divider" />
    {:else if node.type === 'card'}
      {#if node.action}
        <button class="a2ui-card" onclick={() => node.action && act(node.action)}>
          <strong>{node.title}</strong>
          {#if node.text}<span>{node.text}</span>{/if}
          <Icon name="arrow-right" size={15} />
        </button>
      {:else}
        <div class="a2ui-card static"><strong>{node.title}</strong>{#if node.text}<span>{node.text}</span>{/if}</div>
      {/if}
    {/if}
  {/each}
</section>

<style>
  .a2ui {
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 18px;
    border-radius: var(--radius-sm);
    background:
      radial-gradient(22rem 16rem at 102% -10%, color-mix(in srgb, var(--accent) 10%, transparent), transparent 62%),
      var(--surface);
    border: 1px solid var(--line);
    box-shadow: var(--shadow-1);
  }
  .surface-tag {
    display: flex;
    align-items: center;
    gap: 7px;
    color: var(--faint);
    font-size: 0.62rem;
    font-weight: 650;
    letter-spacing: 0.13em;
    text-transform: uppercase;
  }
  .tagmark {
    width: 20px; height: 20px;
    border-radius: 7px;
    display: grid; place-items: center;
    background: var(--accent-soft);
    color: var(--accent);
  }
  .tagver { margin-left: auto; font-family: var(--font-mono); font-size: 0.6rem; letter-spacing: 0; text-transform: none; opacity: 0.72; }
  .surface-head h3 {
    margin: 0;
    font-family: var(--font-display);
    font-size: 1.42rem;
    font-weight: 580;
    letter-spacing: -0.04em;
    line-height: 1.06;
  }
  .surface-head p { margin: 7px 0 0; color: var(--muted); font-size: 0.8rem; line-height: 1.5; }
  .a2ui-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .a2ui-stat {
    display: flex; flex-direction: column; gap: 1px;
    padding: 11px 12px;
    border-radius: 16px;
    background: var(--surface-2);
    border: 1px solid var(--line-soft);
  }
  .a2ui-stat strong { font-family: var(--font-display); font-size: 1.34rem; font-weight: 560; letter-spacing: -0.042em; line-height: 1; }
  .a2ui-stat span { color: var(--faint); font-size: 0.65rem; }
  .a2ui-stat em { color: var(--accent); font-style: normal; font-size: 0.6rem; font-weight: 620; }
  .progress-label { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 6px; color: var(--faint); font-size: 0.68rem; }
  .progress-label strong { color: var(--ink); font-family: var(--font-mono); font-size: 0.68rem; font-weight: 500; }
  .surface-list-title { margin: 0; color: var(--faint); font-size: 0.63rem; font-weight: 660; letter-spacing: 0.14em; text-transform: uppercase; }
  .a2ui-list { display: flex; flex-direction: column; gap: 6px; margin: 0; padding: 0; list-style: none; }
  .a2ui-list li { margin: 0; }
  .rowitem {
    display: flex; align-items: center; gap: 10px; width: 100%;
    padding: 10px 11px;
    text-align: left; color: inherit;
    border-radius: 15px;
    background: var(--surface-2);
    border: 1px solid transparent;
    transition: border-color 200ms ease, background 200ms ease;
  }
  button.rowitem:hover { border-color: color-mix(in srgb, var(--accent) 22%, transparent); background: var(--accent-mist); }
  button.rowitem:active { transform: scale(0.992); }
  .rowtext { flex: 1; min-width: 0; }
  .rowtext strong { display: block; font-size: 0.82rem; font-weight: 630; letter-spacing: -0.006em; }
  .rowtext span { display: block; color: var(--faint); font-size: 0.72rem; line-height: 1.45; margin-top: 1px; }
  .rowtrail { display: inline-flex; align-items: center; gap: 3px; flex: none; color: var(--faint); font-size: 0.68rem; }
  .a2ui-actions { display: flex; gap: 7px; flex-wrap: wrap; }
  .a2ui-actions .btn { min-height: 40px; padding: 9px 15px; font-size: 0.79rem; }
  .a2ui-note { margin: 0; color: var(--faint); font-size: 0.71rem; line-height: 1.5; }
  .a2ui-divider { height: 1px; border: 0; background: var(--line-soft); margin: 2px 0; }
  .a2ui-card {
    display: flex; align-items: center; gap: 10px; width: 100%;
    padding: 13px 14px; text-align: left; color: inherit;
    border-radius: 17px;
    background: var(--surface-2);
    border: 1px solid var(--line-soft);
  }
  .a2ui-card strong { font-size: 0.85rem; font-weight: 630; }
  .a2ui-card span { flex: 1; color: var(--faint); font-size: 0.73rem; }
  .a2ui-card > :global(svg) { color: var(--faint); flex: none; }
  .a2ui-text.display {
    font-family: var(--font-display);
    font-size: clamp(1.7rem, 6.6vw, 2.2rem);
    font-weight: 560;
    letter-spacing: -0.045em;
    line-height: 1.02;
    color: var(--ink);
  }
  .a2ui-text.caption { color: var(--faint); font-size: 0.72rem; }
  .a2ui-text.label { color: var(--faint); font-size: 0.64rem; font-weight: 650; letter-spacing: 0.16em; text-transform: uppercase; }
</style>
