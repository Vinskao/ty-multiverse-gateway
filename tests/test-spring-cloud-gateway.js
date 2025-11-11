#!/usr/bin/env node

/**
 * 测试方案B：纯 Spring Cloud Gateway 架构
 * 
 * 架构：前端 → Spring Cloud Gateway Routes → Backend REST Controllers
 */

const GATEWAY_BASE = 'http://localhost:8082/tymg';
const BACKEND_BASE = 'http://localhost:8080/tymb';

const tests = [
  // ========================================
  // People Module Tests
  // ========================================
  {
    name: 'People - Get All',
    method: 'POST',
    url: `${GATEWAY_BASE}/people/get-all`,
    expectedStatus: [200, 202],
    description: 'Gateway Route → Backend PeopleController.getAllPeople()'
  },
  {
    name: 'People - Insert',
    method: 'POST',
    url: `${GATEWAY_BASE}/people/insert`,
    body: {
      name: 'TestCharacter',
      description: 'Test',
      bonus: 10,
      ability: 'Test Ability'
    },
    expectedStatus: [201, 400],
    description: 'Gateway Route → Backend PeopleController.insertPeople()'
  },

  // ========================================
  // Weapons Module Tests
  // ========================================
  {
    name: 'Weapons - Get All',
    method: 'GET',
    url: `${GATEWAY_BASE}/weapons`,
    expectedStatus: [200],
    description: 'Gateway Route → Backend WeaponController.getAllWeapons()'
  },

  // ========================================
  // Gallery Module Tests
  // ========================================
  {
    name: 'Gallery - Get All',
    method: 'POST',
    url: `${GATEWAY_BASE}/gallery/getAll`,
    expectedStatus: [401], // 需要认证
    description: 'Gateway Route → Backend GalleryController.getAllImages()'
  },

  // ========================================
  // Deckofcards (Blackjack) Tests
  // ========================================
  {
    name: 'Blackjack - Status',
    method: 'GET',
    url: `${GATEWAY_BASE}/deckofcards/blackjack/status`,
    expectedStatus: [401], // 需要认证
    description: 'Gateway Route → Backend BlackjackController.getStatus()'
  },

  // ========================================
  // Damage Calculation
  // ========================================
  {
    name: 'People - Damage Calculation',
    method: 'GET',
    url: `${GATEWAY_BASE}/people/damageWithWeapon?name=TestCharacter`,
    expectedStatus: [200, 400],
    description: 'Gateway Route → Backend WeaponDamageController.calculateDamageWithWeapon()'
  },

  // ========================================
  // People Names
  // ========================================
  {
    name: 'People - Get Names',
    method: 'GET',
    url: `${GATEWAY_BASE}/people/names`,
    expectedStatus: [200],
    description: 'Gateway Route → Backend PeopleController.getNames()'
  },

  // ========================================
  // Infrastructure Routes
  // ========================================
  {
    name: 'Health Consumer Check',
    method: 'GET',
    url: `${GATEWAY_BASE}/health/consumer`,
    expectedStatus: [200, 500],
    description: 'Gateway Route → Backend HealthConsumerController'
  },
];

async function testEndpoint(test) {
  try {
    console.log(`\n${'='.repeat(80)}`);
    console.log(`📝 测试: ${test.name}`);
    console.log(`🔗 URL: ${test.url}`);
    console.log(`📊 方法: ${test.method}`);
    console.log(`📋 流程: ${test.description}`);
    console.log(`${'='.repeat(80)}`);

    const options = {
      method: test.method,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    };

    if (test.body && test.method !== 'GET') {
      options.body = JSON.stringify(test.body);
    }

    const startTime = Date.now();
    const response = await fetch(test.url, options);
    const duration = Date.now() - startTime;

    let data;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      data = await response.text();
    }

    const passed = test.expectedStatus.includes(response.status);
    const status = passed ? '✅ PASS' : '❌ FAIL';

    console.log(`\n${status}`);
    console.log(`📡 状态码: ${response.status} ${response.statusText}`);
    console.log(`⏱️  响应时间: ${duration}ms`);
    
    if (response.ok) {
      console.log(`✅ Spring Cloud Gateway 成功转发请求到 Backend REST API`);
      if (typeof data === 'object') {
        console.log(`📦 响应数据类型: ${Array.isArray(data) ? 'Array' : 'Object'}`);
        if (Array.isArray(data)) {
          console.log(`📊 数组长度: ${data.length}`);
        }
      }
    } else {
      console.log(`⚠️  响应内容预览: ${typeof data === 'string' ? data.substring(0, 200) : JSON.stringify(data).substring(0, 200)}`);
    }

    return { test: test.name, passed, status: response.status, duration, data };
  } catch (error) {
    console.log(`\n❌ 网络错误`);
    console.log(`🔴 错误: ${error.message}`);
    return { test: test.name, passed: false, error: error.message };
  }
}

async function main() {
  console.log('\n🚀 开始测试：纯 Spring Cloud Gateway 架构\n');
  console.log(`Gateway 地址: ${GATEWAY_BASE}`);
  console.log(`Backend 地址: ${BACKEND_BASE}`);
  console.log(`测试数量: ${tests.length}`);
  console.log(`\n架构说明:`);
  console.log(`  前端 → Spring Cloud Gateway (WebFlux)`);
  console.log(`       ↓ (HTTP Routes)`);
  console.log(`  Backend REST Controllers`);
  console.log(`       ↓ (Service Layer)`);
  console.log(`  Database\n`);

  const results = [];
  for (const test of tests) {
    const result = await testEndpoint(test);
    results.push(result);
    await new Promise(resolve => setTimeout(resolve, 500)); // 延迟避免过载
  }

  // 统计
  console.log(`\n${'='.repeat(80)}`);
  console.log('📊 测试总结');
  console.log(`${'='.repeat(80)}`);

  const passed = results.filter(r => r.passed).length;
  const failed = results.filter(r => !r.passed).length;
  const total = results.length;

  console.log(`\n总测试数: ${total}`);
  console.log(`✅ 通过: ${passed}`);
  console.log(`❌ 失败: ${failed}`);
  console.log(`📈 成功率: ${((passed / total) * 100).toFixed(2)}%`);

  if (passed === total) {
    console.log('\n🎉 所有测试通过！纯 Spring Cloud Gateway 架构工作正常');
    console.log('✅ 重构成功：');
    console.log('   - Gateway: 使用 Spring Cloud Gateway Routes (WebFlux)');
    console.log('   - Backend: REST Controllers 正常工作');
    console.log('   - 无需 gRPC 复杂度');
  } else {
    console.log('\n⚠️  部分测试失败，请检查：');
    console.log('   - Backend 是否启动？(Port 8080)');
    console.log('   - Gateway 是否启动？(Port 8082)');
    console.log('   - 路由配置是否正确？');
    
    console.log('\n失败的测试：');
    results.filter(r => !r.passed).forEach(r => {
      console.log(`   ❌ ${r.test} - ${r.error || `Status: ${r.status}`}`);
    });
  }

  console.log('\n');
}

main().catch(error => {
  console.error('❌ 测试脚本执行失败:', error);
  process.exit(1);
});

