<script lang="ts">
  import Empty from '../components/Empty.svelte';
  import Icon from '../components/Icon.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { toasts } from '../lib/toast.svelte';
  import type { Course } from '../lib/types';
  import { percent, relative } from '../lib/util';

  const slug = $derived(router.segments[1] ?? '');
  const course = $derived(slug ? union.courses.find((c) => c.slug === slug) : undefined);
  const enrollment = $derived(course ? union.enrollmentFor(course.id) : undefined);
  const progress = $derived(course ? union.courseProgress(course) : 0);

  let lessonId = $state('');
  let quizChoice = $state(-1);
  let busy = $state(false);

  const lesson = $derived.by(() => {
    if (!course) return undefined;
    return course.lessons.find((l) => l.id === lessonId) ?? course.lessons[0];
  });

  const lessonIndex = $derived(course && lesson ? course.lessons.findIndex((l) => l.id === lesson.id) : -1);
  const isDone = $derived(Boolean(lesson && enrollment?.done.includes(lesson.id)));

  $effect(() => {
    // A knowledge check belongs to one lesson: never carry a choice across.
    if (lesson?.id) quizChoice = -1;
  });

  async function start(target: Course) {
    busy = true;
    try {
      await union.enroll(target);
      router.go(`/learning/${target.slug}`);
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function complete() {
    if (!course || !lesson) return;
    if (lesson.quiz && quizChoice < 0) {
      toasts.push('Choose an answer before completing this lesson.', 'warn');
      return;
    }
    busy = true;
    try {
      await union.completeLesson(course, lesson.id, lesson.quiz ? quizChoice : undefined);
      if (lesson.quiz && quizChoice !== lesson.quiz.answer) {
        toasts.push('Lesson recorded. The reasoning note above is worth a second read.', 'info');
      } else {
        toasts.push('Lesson completed and recorded on this device.', 'success');
      }
      const next = course.lessons[lessonIndex + 1];
      if (next) lessonId = next.id;
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  function goLesson(id: string) {
    lessonId = id;
    quizChoice = -1;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
</script>

{#if course}
  <TopBar title={course.title} subtitle={`${course.level} · ${course.minutes} min`} back fallback="/learning" />
  <main class="screen">
    <section class="card pad stack">
      <div class="row between">
        <span class="chip accent"><Icon name="book" size={12} /> Course</span>
        <span class="chip">{progress}% complete</span>
      </div>
      <div>
        <h1 class="display">{course.title}</h1>
        <p class="muted small" style="margin:4px 0 0">{course.subtitle}</p>
      </div>
      <div class="progress" style={`--p:${progress}`}><span></span></div>
      <p class="small muted" style="margin:0">{course.about}</p>
      {#if !enrollment}
        <button class="btn primary block" onclick={() => start(course)} disabled={busy}>
          <Icon name="play" size={15} /> Start this course
        </button>
      {:else if enrollment.completedAt}
        <div class="banner good"><Icon name="check" size={17} /><div><strong>Course complete.</strong> Finished {relative(enrollment.completedAt)}. The record is yours.</div></div>
      {/if}
    </section>

    <section class="section">
      <div class="section-head"><h2>Lessons</h2><span class="hint">{enrollment?.done.length ?? 0}/{course.lessons.length} done</span></div>
      <div class="list">
        {#each course.lessons as l, i (l.id)}
          {@const done = enrollment?.done.includes(l.id)}
          <button class="listitem" class:active={lesson?.id === l.id} onclick={() => goLesson(l.id)}>
            <span class="lessonnum" class:done>{#if done}<Icon name="check" size={14} />{:else}{i + 1}{/if}</span>
            <div class="main"><strong>{l.title}</strong><span>{l.minutes} min{l.quiz ? ' · knowledge check' : ''}</span></div>
            <span class="trail"><Icon name="chevron-right" size={16} /></span>
          </button>
        {/each}
      </div>
    </section>

    {#if lesson}
      <section class="section card pad lessonbody">
        <div class="row between">
          <div class="eyebrow">Lesson {lessonIndex + 1} of {course.lessons.length}</div>
          {#if isDone}<span class="chip green"><Icon name="check" size={11} /> Completed</span>{/if}
        </div>
        <h2 class="display">{lesson.title}</h2>
        <div class="markdown">
          {#each lesson.body.split(/\n\n+/g) as para}
            {#if para.startsWith('## ')}
              <h3>{para.slice(3)}</h3>
            {:else if para.startsWith('- ')}
              <ul>{#each para.split(/\n/g) as item}<li>{item.replace(/^- /, '')}</li>{/each}</ul>
            {:else}
              <p>{para}</p>
            {/if}
          {/each}
        </div>

        {#if lesson.practice}
          <div class="banner info"><Icon name="feather" size={17} /><div><strong>Practice.</strong> {lesson.practice}</div></div>
        {/if}

        {#if lesson.quiz}
          <div class="quiz">
            <p class="label">{lesson.quiz.question}</p>
            {#each lesson.quiz.options as option, i (option)}
              <label class="check" class:checked={quizChoice === i}>
                <input type="radio" name={`quiz-${lesson.id}`} checked={quizChoice === i} onchange={() => (quizChoice = i)} />
                <span>{option}</span>
              </label>
            {/each}
            {#if isDone && enrollment?.answers[lesson.id] !== undefined}
              <p class="hint">Recorded answer: {lesson.quiz.options[enrollment.answers[lesson.id]]}. {lesson.quiz.why}</p>
            {/if}
          </div>
        {/if}

        <div class="row wrap">
          {#if lessonIndex > 0}
            <button class="btn ghost" onclick={() => goLesson(course.lessons[lessonIndex - 1].id)}><Icon name="arrow-left" size={15} /> Previous</button>
          {/if}
          <button class="btn primary push" onclick={complete} disabled={busy || isDone}>
            {#if isDone}<Icon name="check" size={15} /> Recorded{:else}<Icon name="check" size={15} /> Complete lesson{/if}
          </button>
          {#if lessonIndex < course.lessons.length - 1}
            <button class="btn" onclick={() => goLesson(course.lessons[lessonIndex + 1].id)}>Next <Icon name="arrow-right" size={15} /></button>
          {/if}
        </div>
      </section>
    {/if}
  </main>
{:else}
  <TopBar title="Learning" subtitle="Practical courses, verified progress" back fallback="/more" />
  <main class="screen">
    <div class="banner info">
      <Icon name="book" size={17} />
      <div><strong>Learning is demonstrated, not advertised.</strong> Every lesson ends in a practice you can perform, and progress is written to this device.</div>
    </div>

    {#if union.courses.length === 0}
      <Empty icon="book" title="No courses yet" text="Official courses are seeded when your identity is created." />
    {:else}
      <section class="section">
        <div class="section-head"><h2>Courses</h2><span class="hint">{union.courses.length} available</span></div>
        <div class="list">
          {#each union.courses as c (c.id)}
            {@const e = union.enrollmentFor(c.id)}
            {@const p = union.courseProgress(c)}
            <button class="listitem courseitem" onclick={() => router.go(`/learning/${c.slug}`)}>
              <span class="courseico" style={`--tint:${c.color}`}><Icon name="book" size={18} /></span>
              <div class="main">
                <strong>{c.title}</strong>
                <span>{c.subtitle}</span>
                <div class="row" style="gap:8px;margin-top:7px">
                  <div class="progress" style={`--p:${p}`}><span></span></div>
                  <span class="tiny faint nowrap">{p}%</span>
                </div>
              </div>
              <span class="trail">
                {#if e?.completedAt}<span class="chip green"><Icon name="check" size={11} /> Done</span>
                {:else if !e}<Icon name="chevron-right" size={16} />
                {:else}<span class="chip accent">{e.done.length}/{c.lessons.length}</span>{/if}
              </span>
            </button>
          {/each}
        </div>
      </section>
    {/if}

    <p class="fine center">Courses are authored for Union and stored locally. No completion data is sold, ranked or shown to anyone unless you choose to share it.</p>
  </main>
{/if}

<style>
  .listitem.active { border-color: color-mix(in srgb, var(--accent) 42%, var(--line)); background: color-mix(in srgb, var(--accent) 6%, var(--panel)); }
  .lessonnum {
    width: 28px; height: 28px; border-radius: 50%; flex: none;
    display: grid; place-items: center; font-size: 0.74rem; font-weight: 650;
    background: var(--panel-2); border: 1px solid var(--line); color: var(--muted);
  }
  .lessonnum.done { background: color-mix(in srgb, var(--green) 13%, var(--panel)); color: var(--green); border-color: color-mix(in srgb, var(--green) 25%, var(--line)); }
  .courseico {
    width: 42px; height: 42px; border-radius: 12px; flex: none;
    display: grid; place-items: center;
    background: color-mix(in srgb, var(--tint) 16%, var(--panel));
    border: 1px solid color-mix(in srgb, var(--tint) 32%, var(--line));
    color: color-mix(in srgb, var(--tint) 75%, white);
  }
  .courseitem .main .progress { width: 150px; flex: none; }
  .lessonbody { display: flex; flex-direction: column; gap: 14px; margin-top: 26px; }
  .lessonbody h2 { margin: 0; font-size: 1.45rem; }
  .quiz { display: flex; flex-direction: column; gap: 7px; }
  .quiz p.label { margin: 0 0 2px; }
  .fine { margin-top: 20px; }
</style>
