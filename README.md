# Java Multiple Inheritance (Annotation Processor)

Проект реализует множественное наследование в Java через генерацию исходников на этапе компиляции.

## Что есть в проекте

- `@RootHierarchy` — помечает базовый интерфейс и генерирует `*Root`.
- `@UseMultipleInheritance` — помечает marker-класс и генерирует `*MI`.
- `MultipleInheritanceFactory` — создаёт экземпляр сгенерированного класса, чтобы пользователю не нужно было знать имя `*MI`.

## Использование фабрики

1. Опишите корневой интерфейс:

```java
@RootHierarchy
public interface A {
    String method();
}
```

2. Создайте классы-ветки, унаследованные от `ARoot`:

```java
public class B extends ARoot {
    @Override
    public String method() {
        return "B->" + nextMethod();
    }
}
```

3. Добавьте marker-класс:

```java
@UseMultipleInheritance(root = A.class, targets = {D.class, B.class, C.class})
public class Marker {}
```

4. Создайте объект через фабрику:

```java
A instance = MultipleInheritanceFactory.create(
        Marker.class,
        A.class,
        new D(),
        new B(),
        new C()
);
```

## Smoke: diamond problem

В `work/smoke` лежит пример ромбовидного наследования:

- `A` — базовый интерфейс
- `B` и `C` — независимые ветки от `A`
- `D` — финальный класс
- `Marker` — конфигурация множественного наследования
- `SmokeRun` — запуск через `MultipleInheritanceFactory`

Ожидаемый результат выполнения примера:

```text
D->B->C
```

## Как запустить

Из директории `work` (рекомендуемый способ, JDK 21):

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 21)" PATH="$JAVA_HOME/bin:$PATH" ./gradlew clean smokeRun
```

Это выполнит полный цикл для smoke-примера:
- соберёт основной код и процессор,
- скомпилирует файлы из `smoke`,
- запустит `smoke.SmokeRun`.

Ожидаемый вывод:

```text
D->B->C
```

Если у вас по умолчанию более новая Java (например 26), запускайте Gradle с JDK 21 как в примере выше.
